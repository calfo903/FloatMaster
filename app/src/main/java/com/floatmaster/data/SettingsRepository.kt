package com.floatmaster.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode") // system, light, dark
        val KEY_ACCENT = stringPreferencesKey("accent")
        val KEY_SHOW_DOCK = booleanPreferencesKey("show_dock")
        val KEY_SNAP_TO_EDGE = booleanPreferencesKey("snap_to_edge")
        val KEY_TRANSPARENCY_DEFAULT = floatPreferencesKey("transparency")
        val KEY_HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val KEY_HAPTICS = booleanPreferencesKey("haptics")
    }

    val darkMode: Flow<String> = context.settingsDataStore.data.map { it[KEY_DARK_MODE] ?: "system" }
    val showDock: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_SHOW_DOCK] ?: true }
    val snapToEdge: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_SNAP_TO_EDGE] ?: true }

    suspend fun setDarkMode(value: String) { context.settingsDataStore.edit { it[KEY_DARK_MODE] = value } }
    suspend fun setShowDock(value: Boolean) { context.settingsDataStore.edit { it[KEY_SHOW_DOCK] = value } }
}
