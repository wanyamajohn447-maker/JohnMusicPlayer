package com.wanyamajohn.musicplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        PlaylistEntity::class, PlaylistSongEntity::class, FavouriteEntity::class,
        PlaybackHistoryEntity::class, PlaybackQueueEntity::class, LyricsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        /** Add a Migration here for every schema version bump. User data is never wiped silently. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
