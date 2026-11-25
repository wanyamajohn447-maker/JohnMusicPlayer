package com.wanyamajohn.musicplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.wanyamajohn.musicplayer.core.LrcParser
import com.wanyamajohn.musicplayer.data.db.LyricsDao
import com.wanyamajohn.musicplayer.data.db.LyricsEntity
import com.wanyamajohn.musicplayer.data.prefs.PrefKeys
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Local lyrics only: manual files, or .lrc/.txt files found in a folder the user chose. No online lookup. */
@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LyricsDao,
    private val prefs: PreferencesRepository,
) {
    fun observe(songId: Long): Flow<LyricsEntity?> = dao.observe(songId)
    suspend fun get(songId: Long): LyricsEntity? = dao.get(songId)
    suspend fun save(songId: Long, text: String) = dao.upsert(LyricsEntity(songId, text, LrcParser.isSynced(text)))
    suspend fun delete(songId: Long) = dao.delete(songId)

    suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
    }

    /** Looks for "<song file name>.lrc" (then .txt) in the user-selected lyrics folder. */
    suspend fun autoLoad(song: Song): Boolean = withContext(Dispatchers.IO) {
        val tree = prefs.get(PrefKeys.LYRICS_TREE, "")
        if (tree.isEmpty() || song.displayName.isEmpty()) return@withContext false
        val base = song.displayName.substringBeforeLast('.')
        val dir = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(tree)) }.getOrNull() ?: return@withContext false
        val file = runCatching { dir.findFile("$base.lrc") ?: dir.findFile("$base.txt") }.getOrNull() ?: return@withContext false
        val text = readText(file.uri) ?: return@withContext false
        if (text.isBlank()) return@withContext false
        save(song.id, text)
        true
    }
}
