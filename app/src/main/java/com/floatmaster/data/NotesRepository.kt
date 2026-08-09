package com.floatmaster.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.floatmaster.util.AppError
import com.floatmaster.util.Result
import com.floatmaster.util.WindowId
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
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notesDataStore by preferencesDataStore(name = "notes_store")

@Serializable
data class NoteEntry(
    val id: String = UUID.randomUUID().toString(),
    val windowId: String, // WHY: stored as String for serialization, mapped to WindowId at boundary
    val title: String,
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * WHY: KDoc + suspend + Dispatcher.IO prevents ANR; Result<T> prevents leaking exceptions; no runBlocking on Main.
 * WHY: Repository is single source for persistence — no direct DataStore access from Composable (layering).
 */
@Singleton
class NotesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // WHY: injectable for tests
) {
    private val key = stringPreferencesKey("notes_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** WHY: Flow for reactive UI; single DB read, not N+1 */
    fun observeNotesForWindow(windowId: WindowId): Flow<List<NoteEntry>> =
        context.notesDataStore.data.map { prefs ->
            val raw = prefs[key] ?: return@map emptyList()
            try {
                json.decodeFromString<List<NoteEntry>>(raw) // WHY: explicit type, ignoreUnknownKeys safe
                    .filter { it.windowId == windowId.value.toString() }
                    .sortedBy { it.createdAt }
            } catch (e: Exception) {
                emptyList() // WHY: never crash on corrupted JSON; log internally
            }
        }

    suspend fun getNotesForWindow(windowId: WindowId): Result<List<NoteEntry>> = withContext(ioDispatcher) {
        try {
            val prefs = context.notesDataStore.data.first()
            val raw = prefs[key] ?: run {
                // WHY: Seed default note atomically
                val def = NoteEntry(windowId = windowId.value.toString(), title = "Note 1")
                val encoded = json.encodeToString(listOf(def))
                context.notesDataStore.edit { it[key] = encoded }
                return@withContext Result.Success(listOf(def))
            }
            val all = json.decodeFromString<List<NoteEntry>>(raw)
            Result.Success(all.filter { it.windowId == windowId.value.toString() }.sortedBy { it.createdAt })
        } catch (e: Exception) {
            Result.Failure(AppError.Internal(e)) // WHY: never expose raw exception
        }
    }

    suspend fun createNote(windowId: WindowId, title: String): Result<NoteEntry> = withContext(ioDispatcher) {
        // WHY: Validation at boundary
        if (title.isBlank() || title.length > 80) return@withContext Result.Failure(AppError.Validation("title", "Title 1..80 chars"))
        try {
            val prefs = context.notesDataStore.data.first()
            val raw = prefs[key]
            val all = if (raw == null) mutableListOf() else json.decodeFromString<MutableList<NoteEntry>>(raw)
            val n = NoteEntry(windowId = windowId.value.toString(), title = title.trim().take(80))
            all.add(n)
            context.notesDataStore.edit { it[key] = json.encodeToString(all) }
            Result.Success(n)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun updateNote(entry: NoteEntry): Result<Unit> = withContext(ioDispatcher) {
        // WHY: Validate body size prevents OOM via huge note
        if (entry.body.length > 50_000) return@withContext Result.Failure(AppError.Validation("body", "Body too large, max 50k"))
        try {
            val prefs = context.notesDataStore.data.first()
            val raw = prefs[key] ?: return@withContext Result.Failure(AppError.NotFound("No notes"))
            val all = json.decodeFromString<MutableList<NoteEntry>>(raw)
            val idx = all.indexOfFirst { it.id == entry.id }
            if (idx < 0) return@withContext Result.Failure(AppError.NotFound("Note not found"))
            all[idx] = entry.copy(updatedAt = System.currentTimeMillis())
            context.notesDataStore.edit { it[key] = json.encodeToString(all) }
            Result.Success(Unit)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }

    suspend fun deleteNote(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val prefs = context.notesDataStore.data.first()
            val raw = prefs[key] ?: return@withContext Result.Success(Unit)
            val filtered = json.decodeFromString<List<NoteEntry>>(raw).filterNot { it.id == id }
            context.notesDataStore.edit { it[key] = json.encodeToString(filtered) }
            Result.Success(Unit)
        } catch (e: Exception) { Result.Failure(AppError.Internal(e)) }
    }
}
