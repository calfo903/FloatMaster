package com.floatmaster.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.floatmaster.model.WindowFavorite
import com.floatmaster.model.WindowType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.historyDataStore by preferencesDataStore(name = "window_history")

class WindowHistoryManager(private val context: Context) {
    private val favKey = stringPreferencesKey("favorites")
    private val histKey = stringPreferencesKey("history")
    private val json = Json { ignoreUnknownKeys = true }

    fun getFavorites(): List<WindowFavorite> = runBlocking {
        val raw = context.historyDataStore.data.first()[favKey] ?: return@runBlocking emptyList()
        try { json.decodeFromString<List<WindowFavorite>>(raw) } catch (_: Exception) { emptyList() }
    }

    fun addFavorite(type: WindowType, title: String, url: String?, pkg: String?) = runBlocking {
        val fav = WindowFavorite(type = type, title = title, url = url, packageName = pkg)
        val list = getFavorites().toMutableList().apply { add(0, fav) }
        context.historyDataStore.edit { it[favKey] = json.encodeToString(list) }
    }

    fun removeFavorite(id: String) = runBlocking {
        val list = getFavorites().filterNot { it.id == id }
        context.historyDataStore.edit { it[favKey] = json.encodeToString(list) }
    }

    fun exportSettings(): String {
        val favs = getFavorites()
        return json.encodeToString(favs)
    }

    fun importSettings(jsonStr: String): Boolean {
        return try {
            val list = json.decodeFromString<List<WindowFavorite>>(jsonStr)
            runBlocking { context.historyDataStore.edit { it[favKey] = json.encodeToString(list) } }
            true
        } catch (_: Exception) { false }
    }
}
