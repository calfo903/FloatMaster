package com.floatmaster.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.floatmaster.util.AppError
import com.floatmaster.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.browserHistoryDataStore by preferencesDataStore(name = "browser_history")

@Serializable
data class HistoryEntry(
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * WHY: Persistent history — survives window close/restart, capped 200, never expose raw exception.
 * Quality: suspend + Dispatchers.IO, Result envelope, KDoc, no !!.
 */
@Singleton
class BrowserHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val key = stringPreferencesKey("browser_history_json")
    private val json = Json { ignoreUnknownKeys = true }

    fun observeHistory(): Flow<List<HistoryEntry>> = context.browserHistoryDataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map emptyList()
        try { json.decodeFromString<List<HistoryEntry>>(raw).sortedByDescending { it.timestamp } }
        catch (_: Exception) { emptyList() }
    }

    suspend fun getHistory(): Result<List<HistoryEntry>> = withContext(ioDispatcher) {
        try {
            val raw = context.browserHistoryDataStore.data.first()[key] ?: return@withContext Result.Success(emptyList())
            Result.Success(json.decodeFromString<List<HistoryEntry>>(raw).sortedByDescending { it.timestamp })
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun add(url: String, title: String): Result<Unit> = withContext(ioDispatcher) {
        // WHY: Validate + truncate prevents OOM / DoS via huge URL
        val safeUrl = url.take(2048).trim()
        if (safeUrl.isBlank() || safeUrl == "about:blank") return@withContext Result.Success(Unit)
        if (safeUrl.startsWith("data:")) return@withContext Result.Success(Unit) // WHY: block data URIs
        val safeTitle = title.take(120).ifBlank { safeUrl }
        try {
            val prefs = context.browserHistoryDataStore.data.first()
            val raw = prefs[key]
            val all = if (raw == null) mutableListOf() else json.decodeFromString<MutableList<HistoryEntry>>(raw)
            // WHY: Dedupe consecutive same URL within 5s
            if (all.firstOrNull()?.url == safeUrl && System.currentTimeMillis() - (all.firstOrNull()?.timestamp ?: 0) < 5000) return@withContext Result.Success(Unit)
            all.add(0, HistoryEntry(url = safeUrl, title = safeTitle))
            val capped = all.take(200) // WHY: cap 200 prevents DataStore 1MB limit
            context.browserHistoryDataStore.edit { it[key] = json.encodeToString(capped) }
            Result.Success(Unit)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun clear(): Result<Unit> = withContext(ioDispatcher) {
        try { context.browserHistoryDataStore.edit { it.remove(key) }; Result.Success(Unit) }
        catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun delete(url: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val prefs = context.browserHistoryDataStore.data.first()
            val raw = prefs[key] ?: return@withContext Result.Success(Unit)
            val filtered = json.decodeFromString<List<HistoryEntry>>(raw).filterNot { it.url == url }
            context.browserHistoryDataStore.edit { it[key] = json.encodeToString(filtered) }
            Result.Success(Unit)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }
}
