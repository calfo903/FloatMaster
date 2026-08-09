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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.clipboardDataStore by preferencesDataStore(name = "clipboard_store")

@Serializable
data class ClipboardEntry(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
) {
    /** WHY: Pure function, no side effect */
    fun timeAgo(): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
}

/**
 * WHY: Suspend + injected dispatcher prevents Main blocking; Result hides internal errors.
 */
@Singleton
class ClipboardRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val key = stringPreferencesKey("clipboard_json")
    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<ClipboardEntry>> = context.clipboardDataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map emptyList()
        try { json.decodeFromString<List<ClipboardEntry>>(raw).sortedWith(compareBy({ !it.isPinned }, { -it.timestamp })) }
        catch (_: Exception) { emptyList() }
    }

    suspend fun getAll(): Result<List<ClipboardEntry>> = withContext(ioDispatcher) {
        try {
            val raw = context.clipboardDataStore.data.first()[key] ?: return@withContext Result.Success(emptyList())
            val list = json.decodeFromString<List<ClipboardEntry>>(raw).sortedWith(compareBy({ !it.isPinned }, { -it.timestamp }))
            Result.Success(list)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun addIfNew(text: String): Result<Unit> = withContext(ioDispatcher) {
        // WHY: Validation prevents storing huge clipboard (images as base64)
        val trimmed = text.trim().take(10_000) // WHY: cap 10k prevents OOM
        if (trimmed.isBlank()) return@withContext Result.Success(Unit)
        if (trimmed.length < 2) return@withContext Result.Success(Unit) // WHY: ignore single chars
        try {
            val prefs = context.clipboardDataStore.data.first()
            val raw = prefs[key]
            val all = if (raw == null) mutableListOf() else json.decodeFromString<MutableList<ClipboardEntry>>(raw)
            if (all.firstOrNull()?.text == trimmed) return@withContext Result.Success(Unit) // WHY: dedupe immediate
            if (all.any { it.text == trimmed && System.currentTimeMillis() - it.timestamp < 5000 }) return@withContext Result.Success(Unit)
            val entry = ClipboardEntry(text = trimmed)
            val newList = (listOf(entry) + all).take(100) // WHY: cap 100 prevents unbounded growth
            context.clipboardDataStore.edit { it[key] = json.encodeToString(newList) }
            Result.Success(Unit)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun togglePin(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val prefs = context.clipboardDataStore.data.first()
            val raw = prefs[key] ?: return@withContext Result.Failure(AppError.NotFound("Empty"))
            val all = json.decodeFromString<MutableList<ClipboardEntry>>(raw)
            val idx = all.indexOfFirst { it.id == id }
            if (idx < 0) return@withContext Result.Failure(AppError.NotFound("Entry not found"))
            all[idx] = all[idx].copy(isPinned = !all[idx].isPinned)
            context.clipboardDataStore.edit { it[key] = json.encodeToString(all) }
            Result.Success(Unit)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val prefs = context.clipboardDataStore.data.first()
            val raw = prefs[key] ?: return@withContext Result.Success(Unit)
            val filtered = json.decodeFromString<List<ClipboardEntry>>(raw).filterNot { it.id == id }
            context.clipboardDataStore.edit { it[key] = json.encodeToString(filtered) }
            Result.Success(Unit)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun clear(): Result<Unit> = withContext(ioDispatcher) {
        try { context.clipboardDataStore.edit { it[key] = json.encodeToString(emptyList<ClipboardEntry>()) }; Result.Success(Unit) }
        catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }
}
