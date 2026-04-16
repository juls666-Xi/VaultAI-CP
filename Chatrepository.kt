package com.offlineai.data.repository

import com.offlineai.data.model.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * ChatRepository
 * --------------
 * Handles:
 *   1. Persisting messages to Room (chat history).
 *   2. Calling the AI model (currently a dummy/stub implementation).
 *
 * In a real app, step 2 would delegate to a native library (e.g., llama.cpp via JNI)
 * that loads and runs the model file from USB storage.
 */
class ChatRepository(private val messageDao: MessageDao) {

    /** Reactive stream of all stored messages, sorted oldest → newest. */
    val messages: Flow<List<Message>> = messageDao.getAllMessages()

    /** Persist a message to the database. */
    suspend fun addMessage(message: Message) {
        messageDao.insert(message)
    }

    /** Clear the full chat history. */
    suspend fun clearHistory() {
        messageDao.deleteAll()
    }

    /** Retrieve all messages once (for export). */
    suspend fun exportMessages(): List<Message> = messageDao.getAllMessagesOnce()

    /**
     * Generate an AI response for [userInput].
     *
     * -----------------------------------------------------------------------
     * STUB IMPLEMENTATION
     * -----------------------------------------------------------------------
     * Replace this function's body with a call to your JNI bridge that
     * invokes the loaded model (e.g., llama_eval() from llama.cpp).
     *
     * Example real-world replacement:
     *   val response = LlamaBridge.generate(
     *       modelPath  = modelPath,
     *       prompt     = userInput,
     *       maxTokens  = maxTokens,
     *       temperature = temperature
     *   )
     *   return response
     * -----------------------------------------------------------------------
     *
     * @param userInput   The text the user typed.
     * @param modelPath   Path to the model file on USB storage.
     * @param temperature Controls randomness (0.0–2.0).
     * @param maxTokens   Upper bound on generated tokens.
     * @return            The AI's reply text.
     */
    suspend fun generateAiResponse(
        userInput: String,
        modelPath: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        // Simulate inference delay proportional to response length
        delay(1200)

        // --- DUMMY AI RESPONSES (replace with real model inference) ---
        val responses = listOf(
            "That's an interesting question! Based on the patterns in my training data, " +
                    "I can offer the following analysis: $userInput seems to relate to " +
                    "a complex topic that deserves careful consideration.",
            "I understand you're asking about \"$userInput\". Let me think about this carefully. " +
                    "There are multiple perspectives to consider here, each with their own merits.",
            "Great point! \"$userInput\" touches on something fundamental. " +
                    "The offline model at $modelPath is processing your query with " +
                    "temperature=$temperature and max_tokens=$maxTokens.",
            "Here is what I know about your query: The topic of \"$userInput\" " +
                    "has been studied extensively. The key insight is that context matters " +
                    "greatly in how we interpret such questions.",
            "Running inference on the offline model... Based on my analysis, " +
                    "\"$userInput\" can be approached from several angles. " +
                    "My recommendation would be to start with the fundamentals."
        )

        return responses.random()
    }
}