package com.wanyamajohn.musicplayer.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {
    @Query(
        "SELECT p.id AS id, p.name AS name, " +
            "(SELECT COUNT(*) FROM playlist_songs WHERE playlistId = p.id) AS songCount " +
            "FROM playlists p ORDER BY p.name COLLATE NOCASE",
    )
    abstract fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Insert abstract suspend fun insertPlaylist(p: PlaylistEntity): Long
    @Query("UPDATE playlists SET name = :name WHERE id = :id") abstract suspend fun rename(id: Long, name: String)
    @Query("DELETE FROM playlists WHERE id = :id") abstract suspend fun delete(id: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :id ORDER BY position")
    abstract fun observeEntries(id: Long): Flow<List<PlaylistSongEntity>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :id ORDER BY position")
    abstract suspend fun entries(id: Long): List<PlaylistSongEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :id")
    abstract suspend fun maxPosition(id: Long): Int

    @Insert abstract suspend fun insertEntries(items: List<PlaylistSongEntity>)
    @Query("DELETE FROM playlist_songs WHERE id = :entryId") abstract suspend fun deleteEntry(entryId: Long)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :id") abstract suspend fun clear(id: Long)
    @Query("SELECT DISTINCT songId FROM playlist_songs") abstract fun observeAllSongIds(): Flow<List<Long>>

    @Transaction
    open suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        var pos = maxPosition(playlistId)
        insertEntries(songIds.map { PlaylistSongEntity(playlistId = playlistId, songId = it, position = ++pos) })
    }

    @Transaction
    open suspend fun reorder(playlistId: Long, orderedEntryIds: List<Long>) {
        val current = entries(playlistId).associateBy { it.id }
        clear(playlistId)
        insertEntries(orderedEntryIds.mapIndexedNotNull { i, id -> current[id]?.copy(position = i) })
    }
}

@Dao
interface FavouriteDao {
    @Query("SELECT songId FROM favourites ORDER BY addedAt DESC") fun observeIds(): Flow<List<Long>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun add(f: FavouriteEntity)
    @Query("DELETE FROM favourites WHERE songId = :id") suspend fun remove(id: Long)
}

@Dao
interface HistoryDao {
    @Insert suspend fun insert(e: PlaybackHistoryEntity)

    @Query("SELECT songId FROM playback_history GROUP BY songId ORDER BY MAX(playedAt) DESC LIMIT 100")
    fun observeRecent(): Flow<List<Long>>

    @Query("SELECT songId FROM playback_history GROUP BY songId ORDER BY COUNT(*) DESC, MAX(playedAt) DESC LIMIT 100")
    fun observeMostPlayed(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM playback_history") fun observeTotalPlays(): Flow<Int>
    @Query("DELETE FROM playback_history") suspend fun clear()
}

@Dao
abstract class QueueDao {
    @Query("SELECT * FROM playback_queue ORDER BY position") abstract suspend fun all(): List<PlaybackQueueEntity>
    @Query("DELETE FROM playback_queue") abstract suspend fun clear()
    @Insert abstract suspend fun insertAll(items: List<PlaybackQueueEntity>)

    @Transaction
    open suspend fun replace(items: List<PlaybackQueueEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE songId = :songId") fun observe(songId: Long): Flow<LyricsEntity?>
    @Query("SELECT * FROM lyrics WHERE songId = :songId") suspend fun get(songId: Long): LyricsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: LyricsEntity)
    @Query("DELETE FROM lyrics WHERE songId = :songId") suspend fun delete(songId: Long)
}
