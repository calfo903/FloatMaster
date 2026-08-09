package com.floatmaster.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.floatmaster.model.FloatingWindow
import com.floatmaster.model.WindowGeometry
import com.floatmaster.model.WindowType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * WHY: Persist only the minimum window metadata required to restore a user session.
 * DataStore writes are transactional and bounded so process/OEM death cannot corrupt a session.
 */
@Singleton
class SessionRepository @Inject constructor(private val context: Context) {
    private val key = stringPreferencesKey("session_windows")
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = true }

    @Serializable
    private data class SavedWindow(
        val type: String,
        val title: String,
        val url: String?,
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val alpha: Float
    )

    suspend fun save(windows: List<FloatingWindow>) {
        // WHY: Cap serialized state to keep Preferences DataStore comfortably below its size limit.
        val toSave = windows
            .asSequence()
            .filter { it.state.name != "CLOSED" }
            .take(20)
            .map {
                SavedWindow(
                    type = it.type.name,
                    title = it.title.take(80),
                    url = it.url?.take(2048),
                    x = it.geometry.x,
                    y = it.geometry.y,
                    w = it.geometry.width,
                    h = it.geometry.height,
                    alpha = it.geometry.alpha.coerceIn(0.3f, 1f)
                )
            }
            .toList()

        context.sessionDataStore.edit { preferences ->
            if (toSave.isEmpty()) preferences.remove(key)
            else preferences[key] = json.encodeToString(toSave)
        }
    }

    suspend fun hasSavedSession(): Boolean = context.sessionDataStore.data.first()[key] != null

    suspend fun restore(): List<FloatingWindow> = runCatching {
        val raw = context.sessionDataStore.data.first()[key] ?: return emptyList()
        json.decodeFromString<List<SavedWindow>>(raw).mapNotNull { saved ->
            runCatching {
                val type = WindowType.fromString(saved.type) ?: return@mapNotNull null
                FloatingWindow(
                    type = type,
                    title = saved.title.take(80),
                    url = saved.url?.take(2048),
                    geometry = WindowGeometry(
                        x = saved.x,
                        y = saved.y,
                        width = saved.w,
                        height = saved.h,
                        alpha = saved.alpha.coerceIn(0.3f, 1f)
                    ),
                    createdAt = Instant.now(),
                    lastFocusedAt = Instant.now()
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    suspend fun clear() {
        context.sessionDataStore.edit { it.remove(key) }
    }
}
