package com.offlineai.data.model
 
/**
 * Holds all user-configurable settings for the app.
 * Persisted via DataStore Preferences.
 */
data class AppSettings(
    /** Temperature controls randomness (0.0 = deterministic, 2.0 = very random). */
    val temperature: Float = 0.7f,
 
    /** Maximum number of tokens the AI model will generate per response. */
    val maxTokens: Int = 512,
 
    /** Whether the app uses a dark colour scheme. */
    val darkMode: Boolean = false,
 
    /** Full path to the selected model file on USB storage. */
    val modelPath: String = ""
)