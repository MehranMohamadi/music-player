package dev.noor.music.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

private val Context.store by preferencesDataStore("noor")
private val Context.queueStore by preferencesDataStore("noor_queue")
private val Context.positionStore by preferencesDataStore("noor_position")

data class Preferences(
    val welcome: Boolean = false,
    val light: Boolean = false,
    val language: String = "system",
    val excluded: Set<String> = emptySet(),
    val grid: Boolean = false,
)

data class SavedPlayback(
    val uris: List<String> = emptyList(),
    val index: Int = 0,
    val position: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = 0,
    val speed: Float = 1f,
)

@Singleton
class Settings @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.store
    private val queueStore = context.queueStore
    private val positionStore = context.positionStore
    private val playbackLock = Mutex()
    private var savedQueue: List<String>? = null
    val flow =
        store.data
            .catch { if (it is java.io.IOException) emit(emptyPreferences()) else throw it }
            .map {
                Preferences(
                    it[booleanPreferencesKey("welcome")] ?: false,
                    it[booleanPreferencesKey("light")] ?: false,
                    it[stringPreferencesKey("language")] ?: "system",
                    it[stringSetPreferencesKey("excluded")] ?: emptySet(),
                    it[booleanPreferencesKey("grid")] ?: false,
                )
            }

    suspend fun boolean(key: String, value: Boolean) {
        store.edit { it[booleanPreferencesKey(key)] = value }
    }

    suspend fun language(value: String) {
        store.edit { it[stringPreferencesKey("language")] = value }
    }

    suspend fun excluded(value: Set<String>) {
        store.edit { it[stringSetPreferencesKey("excluded")] = value }
    }

    suspend fun savePlayback(p: SavedPlayback) = playbackLock.withLock {
        // A large queue changes infrequently; do not rewrite it with every position checkpoint.
        if (savedQueue != p.uris) {
            queueStore.edit { it[stringPreferencesKey("uris")] = JSONArray(p.uris).toString() }
            savedQueue = p.uris.toList()
        }
        positionStore.edit {
            it[stringPreferencesKey("playback")] =
                JSONObject()
                    .put("index", p.index)
                    .put("position", p.position)
                    .put("shuffle", p.shuffle)
                    .put("repeat", p.repeat)
                    .put("speed", p.speed)
                    .toString()
        }
    }

    suspend fun playback(): SavedPlayback = runCatching {
        val obj = JSONObject(positionStore.data.first()[stringPreferencesKey("playback")] ?: "{}")
        val a = JSONArray(queueStore.data.first()[stringPreferencesKey("uris")] ?: "[]")
        val uris = (0 until a.length()).map { a.getString(it) }
        savedQueue = uris
        SavedPlayback(
            uris,
            obj.optInt("index"),
            obj.optLong("position"),
            obj.optBoolean("shuffle"),
            obj.optInt("repeat"),
            obj.optDouble("speed", 1.0).toFloat(),
        )
    }
        .getOrDefault(SavedPlayback())
}
