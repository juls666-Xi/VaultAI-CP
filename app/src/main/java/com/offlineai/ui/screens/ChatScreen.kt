package com.offlineai.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offlineai.ui.components.ChatBubble
import com.offlineai.ui.components.ChatInputBar
import com.offlineai.ui.components.TypingIndicator
import com.offlineai.ui.components.UsbStatusBanner
import com.offlineai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ChatScreen
 * ----------
 * The primary screen of the app. Displays:
 *  - A top app bar with settings and clear-chat actions.
 *  - A USB status banner (shows connection state; connect/disconnect button).
 *  - A scrollable list of chat messages with AI typing indicator.
 *  - A bottom input bar (text field + voice + send).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel    : ChatViewModel,
    onOpenSettings : () -> Unit
) {
    val messages   by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading  by viewModel.isLoading.collectAsStateWithLifecycle()
    val usbState   by viewModel.usbState.collectAsStateWithLifecycle()
    val errorMsg   by viewModel.errorMessage.collectAsStateWithLifecycle()

    var inputText  by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()
    val context    = LocalContext.current

    // ── Auto-scroll to latest message ───────────────────────────────
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                index = messages.size - 1 + if (isLoading) 1 else 0
            )
        }
    }

    // ── Voice input launcher ─────────────────────────────────────────
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) inputText = spokenText
        }
    }

    // ── Audio permission launcher ────────────────────────────────────
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchSpeechRecognizer(speechLauncher, context)
    }

    fun startVoiceInput() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED ->
                launchSpeechRecognizer(speechLauncher, context)
            else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Export snackbar ──────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Offline AI", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text  = "${messages.size} messages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    // Export chat button
                    IconButton(onClick = {
                        scope.launch {
                            val path = viewModel.exportChatAsText()
                            val msg = if (path != null) "Exported to $path" else "Export failed"
                            snackbarHostState.showSnackbar(msg)
                        }
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export chat")
                    }

                    // Clear chat button
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear chat")
                    }

                    // Settings button
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                value         = inputText,
                onValueChange = { inputText = it },
                onSend        = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                onVoiceInput  = { startVoiceInput() },
                isLoading     = isLoading
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── USB Status Banner ────────────────────────────────────────
            UsbStatusBanner(
                state        = usbState,
                onConnect    = { viewModel.connectUsb() },
                onDisconnect = { viewModel.disconnectUsb() }
            )

            // ── Error snackbar ───────────────────────────────────────────
            errorMsg?.let { msg ->
                LaunchedEffect(msg) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.dismissError()
                }
            }

            // ── Message List ─────────────────────────────────────────────
            if (messages.isEmpty() && !isLoading) {
                EmptyStateHint(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state            = listState,
                    modifier         = Modifier.weight(1f),
                    contentPadding   = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = messages,
                        key   = { it.id }
                    ) { message ->
                        AnimatedVisibility(
                            visible = true,
                            enter   = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                        ) {
                            ChatBubble(message = message)
                        }
                    }

                    // Typing indicator appended at the bottom when loading
                    if (isLoading) {
                        item { TypingIndicator() }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Empty state
// ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateHint(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.SmartToy,
                contentDescription = null,
                modifier           = Modifier.size(64.dp),
                tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                text      = "How can I help you today?",
                style     = MaterialTheme.typography.titleMedium,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                text      = "Connect your USB drive with an AI model,\nthen type a message below.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Voice input helper
// ──────────────────────────────────────────────────────────────

private fun launchSpeechRecognizer(
    launcher : androidx.activity.result.ActivityResultLauncher<Intent>,
    context  : android.content.Context
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message…")
    }
    runCatching { launcher.launch(intent) }
}
