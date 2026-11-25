package com.wanyamajohn.musicplayer.di

import android.content.Context
import androidx.room.Room
import com.wanyamajohn.musicplayer.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "john_music.db")
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()

    @Provides fun playlistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
    @Provides fun favouriteDao(db: AppDatabase): FavouriteDao = db.favouriteDao()
    @Provides fun historyDao(db: AppDatabase): HistoryDao = db.historyDao()
    @Provides fun queueDao(db: AppDatabase): QueueDao = db.queueDao()
    @Provides fun lyricsDao(db: AppDatabase): LyricsDao = db.lyricsDao()
}
