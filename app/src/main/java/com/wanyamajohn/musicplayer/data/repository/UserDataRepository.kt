package com.wanyamajohn.musicplayer.data.repository

import com.wanyamajohn.musicplayer.data.db.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Favourites, history and playlists (all stored locally in Room). */
@Singleton
class UserDataRepository @Inject constructor(
    private val favourites: FavouriteDao,
    private val history: HistoryDao,
    private val playlistDao: PlaylistDao,
) {
    val favouriteIds: Flow<List<Long>> = favourites.observeIds()
    val recentIds: Flow<List<Long>> = history.observeRecent()
    val mostPlayedIds: Flow<List<Long>> = history.observeMostPlayed()
    val totalPlays: Flow<Int> = history.observeTotalPlays()
    val playlistSongIds: Flow<List<Long>> = playlistDao.observeAllSongIds()

    suspend fun setFavourite(songId: Long, favourite: Boolean) {
        if (favourite) favourites.add(FavouriteEntity(songId, System.currentTimeMillis())) else favourites.remove(songId)
    }

    suspend fun recordPlay(songId: Long) = history.insert(PlaybackHistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))
    suspend fun clearHistory() = history.clear()

    fun playlists(): Flow<List<PlaylistSummary>> = playlistDao.observePlaylists()
    fun entries(playlistId: Long): Flow<List<PlaylistSongEntity>> = playlistDao.observeEntries(playlistId)
    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim().ifEmpty { "New playlist" }, createdAt = System.currentTimeMillis()))
    suspend fun renamePlaylist(id: Long, name: String) = playlistDao.rename(id, name.trim().ifEmpty { "Playlist" })
    suspend fun deletePlaylist(id: Long) = playlistDao.delete(id)
    suspend fun addToPlaylist(id: Long, songIds: List<Long>) = playlistDao.addSongs(id, songIds)
    suspend fun removeEntry(entryId: Long) = playlistDao.deleteEntry(entryId)
    suspend fun reorder(playlistId: Long, orderedEntryIds: List<Long>) = playlistDao.reorder(playlistId, orderedEntryIds)
}
