package com.wanyamajohn.musicplayer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.johnDataStore: DataStore<Preferences> by preferencesDataStore(name = "john_prefs")

object PrefKeys {
    val ACCENT = intPreferencesKey("accent")
    val GRID = booleanPreferencesKey("grid_layout")
    val HIDDEN_SECTIONS = stringSetPreferencesKey("hidden_home_sections")
    val VIS_STYLE = stringPreferencesKey("visualizer_style")
    val LYRICS_SIZE = intPreferencesKey("lyrics_size")
    val ART_CIRCLE = booleanPreferencesKey("artwork_circle")
    val LYRICS_TREE = stringPreferencesKey("lyrics_tree")
    val BROWSE_TREES = stringSetPreferencesKey("browse_trees")
    val QUEUE_INDEX = intPreferencesKey("queue_index")
    val QUEUE_POS = longPreferencesKey("queue_position")
    val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
    val EQ_LEVELS = stringPreferencesKey("eq_levels")
    val EQ_PRESET = stringPreferencesKey("eq_preset")
    val EQ_CUSTOM = stringPreferencesKey("eq_custom")
    val BASS = intPreferencesKey("bass_strength")
    val VIRT = intPreferencesKey("virtualizer_strength")
    fun videoPosition(uri: String) = longPreferencesKey("vpos_" + uri.hashCode())
}

data class AppSettings(
    val accent: Int = 0xFFFF7A00.toInt(),
    val grid: Boolean = false,
    val hiddenSections: Set<String> = emptySet(),
    val visStyle: String = "Spectrum",
    val lyricsSize: Int = 18,
    val artCircle: Boolean = false,
    val lyricsFolder: String? = null,
)

@Singleton
class PreferencesRepository @Inject constructor(@ApplicationContext context: Context) {
    private val ds = context.johnDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val data: Flow<Preferences> =
        ds.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    fun <T> observe(key: Preferences.Key<T>, default: T): Flow<T> =
        data.map { it[key] ?: default }.distinctUntilChanged()

    suspend fun <T> get(key: Preferences.Key<T>, default: T): T = data.first()[key] ?: default
    suspend fun <T> set(key: Preferences.Key<T>, value: T) { ds.edit { it[key] = value } }
    fun <T> setAsync(key: Preferences.Key<T>, value: T) { scope.launch { set(key, value) } }
    suspend fun <T> remove(key: Preferences.Key<T>) { ds.edit { it.remove(key) } }

    val settings: Flow<AppSettings> = data.map { p ->
        val d = AppSettings()
        AppSettings(
            accent = p[PrefKeys.ACCENT] ?: d.accent,
            grid = p[PrefKeys.GRID] ?: d.grid,
            hiddenSections = p[PrefKeys.HIDDEN_SECTIONS] ?: d.hiddenSections,
            visStyle = p[PrefKeys.VIS_STYLE] ?: d.visStyle,
            lyricsSize = p[PrefKeys.LYRICS_SIZE] ?: d.lyricsSize,
            artCircle = p[PrefKeys.ART_CIRCLE] ?: d.artCircle,
            lyricsFolder = p[PrefKeys.LYRICS_TREE],
        )
    }.distinctUntilChanged()
}
