package com.floatmaster.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.floatmaster.model.FloatingWindow
import com.floatmaster.model.WindowGeometry
import com.floatmaster.model.WindowState
import com.floatmaster.model.WindowType
import com.floatmaster.util.WindowId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * WHY: Session restore — persists windows on close/kill, restores on boot. Prevents 1-star "windows disappeared".
 */
@Singleton
class SessionRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("session_windows")
    private val json = Json { ignoreUnknownKeys = true }

    @kotlinx.serialization.Serializable
    private data class SavedWindow(val type: String, val title: String, val url: String?, val x:Int,val y:Int,val w:Int,val h:Int,val alpha:Float)

    suspend fun save(windows: List<FloatingWindow>) {
        // WHY: cap 20, truncate to prevent DataStore 1MB limit
        val toSave = windows.take(20).map { SavedWindow(it.type.name, it.title, it.url, it.geometry.x,it.geometry.y,it.geometry.width,it.geometry.height, it.geometry.alpha) }
        context.sessionDataStore.edit { it[key] = json.encodeToString(toSave) }
    }

    suspend fun restore(): List<FloatingWindow> {
        return try {
            val raw = context.sessionDataStore.data.first()[key] ?: return emptyList()
            json.decodeFromString<List<SavedWindow>>(raw).mapNotNull {
                runCatching {
                    val type = WindowType.fromString(it.type) ?: return@mapNotNull null
                    FloatingWindow(type=type, title=it.title, url=it.url, geometry=WindowGeometry(it.x,it.y,it.w,it.h,it.alpha), createdAt=Instant.now(), lastFocusedAt=Instant.now())
                }.getOrNull()
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun clear() { context.sessionDataStore.edit { it.remove(key) } }
}
