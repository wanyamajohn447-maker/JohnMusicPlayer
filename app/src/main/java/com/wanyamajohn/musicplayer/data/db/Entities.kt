package com.wanyamajohn.musicplayer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "playlist_songs",
    foreignKeys = [ForeignKey(PlaylistEntity::class, ["id"], ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId"), Index("songId")],
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    /** MediaStore audio _ID. If the file disappears the row stays and shows as unavailable. */
    val songId: Long,
    val position: Int,
)

@Entity(tableName = "favourites")
data class FavouriteEntity(@PrimaryKey val songId: Long, val addedAt: Long)

@Entity(tableName = "playback_history", indices = [Index("songId"), Index("playedAt")])
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

/** Denormalised so the queue can be restored without a media scan. */
@Entity(tableName = "playback_queue")
data class PlaybackQueueEntity(
    @PrimaryKey val position: Int,
    val mediaId: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String?,
)

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val songId: Long,
    val text: String,
    val isSynced: Boolean,
)

data class PlaylistSummary(val id: Long, val name: String, val songCount: Int)
