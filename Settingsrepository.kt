package com.offlineai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.offlineai.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property: creates a single DataStore instance tied to the Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Persists and retrieves user settings using Jetpack DataStore.
 * DataStore is the modern replacement for SharedPreferences — it's coroutine-safe
 * and exposes settings as a Flow so the UI reacts to changes automatically.
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    // ---------------------------------------------------------------------------
    // Keys
    // ---------------------------------------------------------------------------
    companion object Keys {
        val TEMPERATURE  = floatPreferencesKey("temperature")
        val MAX_TOKENS   = intPreferencesKey("max_tokens")
        val DARK_MODE    = booleanPreferencesKey("dark_mode")
        val MODEL_PATH   = stringPreferencesKey("model_path")
    }

    // ---------------------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------------------

    /** Reactive stream of the current [AppSettings]. Emits defaults if not yet saved. */
    val settings: Flow<AppSettings> = dataStore.data
        .catch { e ->
            // Gracefully handle corrupt DataStore by resetting to defaults
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            AppSettings(
                temperature = prefs[TEMPERATURE] ?: 0.7f,
                maxTokens   = prefs[MAX_TOKENS]  ?: 512,
                darkMode    = prefs[DARK_MODE]   ?: false,
                modelPath   = prefs[MODEL_PATH]  ?: ""
            )
        }

    // ---------------------------------------------------------------------------
    // Write
    // ---------------------------------------------------------------------------

    suspend fun setTemperature(value: Float) {
        dataStore.edit { it[TEMPERATURE] = value.coerceIn(0f, 2f) }
    }

    suspend fun setMaxTokens(value: Int) {
        dataStore.edit { it[MAX_TOKENS] = value.coerceIn(1, 4096) }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setModelPath(path: String) {
        dataStore.edit { it[MODEL_PATH] = path }
    }
}