package com.offlineai.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offlineai.data.model.AppSettings
import com.offlineai.data.model.Message
import com.offlineai.data.model.UsbConnectionState
import com.offlineai.data.repository.AppDatabase
import com.offlineai.data.repository.ChatRepository
import com.offlineai.data.repository.SettingsRepository
import com.offlineai.usb.UsbDeviceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ChatViewModel
 * -------------
 * The single source of truth for the chat screen and USB state.
 *
 * Responsibilities:
 *  - Expose [messages] (from Room) for the UI to render.
 *  - Accept user input, save it, call AI, save AI reply.
 *  - Bridge [UsbDeviceManager] → UI-visible [usbState].
 *  - Manage [isLoading] while AI is generating a response.
 *  - Handle chat export to a plain-text file.
 *
 * Uses [AndroidViewModel] (instead of plain ViewModel) because we need
 * Application context to build the database and USB manager.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    // ──────────────────────────────────────────────────────────────
    // Dependencies
    // ──────────────────────────────────────────────────────────────

    private val db             = AppDatabase.getInstance(application)
    private val chatRepository = ChatRepository(db.messageDao())
    val settingsRepository     = SettingsRepository(application)
    val usbManager             = UsbDeviceManager(application)

    // ──────────────────────────────────────────────────────────────
    // State exposed to the UI
    // ──────────────────────────────────────────────────────────────

    /** All chat messages, sorted oldest → newest. */
    val messages: StateFlow<List<Message>> = chatRepository.messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** USB connection status. */
    val usbState: StateFlow<UsbConnectionState> = usbManager.connectionState

    /** True while waiting for the AI to produce a response. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** User-visible error messages (null = no error). */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Current app settings (temperature, dark mode, etc.). */
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    // ──────────────────────────────────────────────────────────────
    // Init
    // ──────────────────────────────────────────────────────────────

    init {
        // Start listening for USB events as soon as the ViewModel is created
        usbManager.register()
    }

    override fun onCleared() {
        super.onCleared()
        usbManager.unregister()
    }

    // ──────────────────────────────────────────────────────────────
    // Chat actions
    // ──────────────────────────────────────────────────────────────

    /**
     * Send [userInput] as a user message and trigger AI response generation.
     *
     * Flow:
     *  1. Validate input.
     *  2. Save user message to Room.
     *  3. Set [_isLoading] = true.
     *  4. Determine the model path (from USB or settings).
     *  5. Call [ChatRepository.generateAiResponse].
     *  6. Save AI response to Room.
     *  7. Set [_isLoading] = false.
     */
    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return

        viewModelScope.launch {
            // 1. Save the user's message
            val userMessage = Message(content = userInput.trim(), isUser = true)
            chatRepository.addMessage(userMessage)

            // 2. Start loading indicator
            _isLoading.value = true

            try {
                // 3. Determine model path (prefer USB, fall back to saved settings)
                val currentSettings = settings.value
                val modelPath = when (val state = usbState.value) {
                    is UsbConnectionState.Connected -> state.modelPath
                    else -> currentSettings.modelPath.ifBlank { "No model loaded" }
                }

                // 4. Generate AI response (replace body of generateAiResponse with JNI call)
                val aiText = chatRepository.generateAiResponse(
                    userInput   = userInput,
                    modelPath   = modelPath,
                    temperature = currentSettings.temperature,
                    maxTokens   = currentSettings.maxTokens
                )

                // 5. Save AI response
                val aiMessage = Message(content = aiText, isUser = false)
                chatRepository.addMessage(aiMessage)

            } catch (e: Exception) {
                _errorMessage.value = "AI error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Clear all messages from the database. */
    fun clearChat() {
        viewModelScope.launch { chatRepository.clearHistory() }
    }

    fun dismissError() { _errorMessage.value = null }

    // ──────────────────────────────────────────────────────────────
    // USB actions
    // ──────────────────────────────────────────────────────────────

    /** Called when the user taps "Connect to Offline AI". */
    fun connectUsb() { usbManager.connectToUsbDevice() }

    /** Disconnect from the current USB device. */
    fun disconnectUsb() { usbManager.disconnect() }

    /** Returns a list of model file paths found on the USB volume. */
    fun scanUsbForModels(): List<String> = usbManager.findModelFilesOnUsb()

    // ──────────────────────────────────────────────────────────────
    // Settings actions
    // ──────────────────────────────────────────────────────────────

    fun setTemperature(value: Float) {
        viewModelScope.launch { settingsRepository.setTemperature(value) }
    }

    fun setMaxTokens(value: Int) {
        viewModelScope.launch { settingsRepository.setMaxTokens(value) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkMode(enabled) }
    }

    fun setModelPath(path: String) {
        viewModelScope.launch { settingsRepository.setModelPath(path) }
    }

    // ──────────────────────────────────────────────────────────────
    // Export
    // ──────────────────────────────────────────────────────────────

    /**
     * Export the full chat history as a plain .txt file to the
     * device's Downloads folder.
     *
     * @return The absolute path of the exported file, or null on failure.
     */
    suspend fun exportChatAsText(): String? {
        return try {
            val messages = chatRepository.exportMessages()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val sb = StringBuilder("Offline AI — Chat Export\n")
            sb.append("Exported: ${sdf.format(Date())}\n")
            sb.append("=".repeat(50)).append("\n\n")

            messages.forEach { msg ->
                val sender = if (msg.isUser) "You" else "AI"
                val time   = sdf.format(Date(msg.timestamp))
                sb.append("[$time] $sender:\n${msg.content}\n\n")
            }

            // Write to Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val fileName = "chat_export_${System.currentTimeMillis()}.txt"
            val file = File(downloadsDir, fileName)
            file.writeText(sb.toString())
            file.absolutePath

        } catch (e: Exception) {
            _errorMessage.value = "Export failed: ${e.message}"
            null
        }
    }
}
