package com.waycairn.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "waycairn_settings")

/**
 * Preferences DataStore wrapper: the global overlay message, the theme mode, and the running
 * unlock counter used by the every-3rd-unlock notification (Phase 6). All reads are Flows; all
 * writes are suspending and run on DataStore's own IO dispatcher.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val GLOBAL_MESSAGE = stringPreferencesKey("global_message")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val UNLOCK_COUNT = intPreferencesKey("unlock_count")
    }

    val globalMessage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.GLOBAL_MESSAGE] ?: DEFAULT_GLOBAL_MESSAGE
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    val unlockCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.UNLOCK_COUNT] ?: 0
    }

    suspend fun setGlobalMessage(message: String) {
        context.dataStore.edit { it[Keys.GLOBAL_MESSAGE] = message }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUnlockCount(count: Int) {
        context.dataStore.edit { it[Keys.UNLOCK_COUNT] = count }
    }

    /** Atomically increment and return the new unlock count. */
    suspend fun incrementUnlockCount(): Int {
        var updated = 0
        context.dataStore.edit { prefs ->
            updated = (prefs[Keys.UNLOCK_COUNT] ?: 0) + 1
            prefs[Keys.UNLOCK_COUNT] = updated
        }
        return updated
    }

    companion object {
        const val DEFAULT_GLOBAL_MESSAGE = "Before you continue — a few stones still to stack today."
    }
}
