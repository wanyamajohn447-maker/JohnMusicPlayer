package com.wanyamajohn.musicplayer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wanyamajohn.musicplayer.data.db.AppDatabase
import com.wanyamajohn.musicplayer.data.db.PlaylistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
    }
    @After fun tearDown() = db.close()

    @Test fun addReorderRemoveAndCascade() = runBlocking {
        val dao = db.playlistDao()
        val id = dao.insertPlaylist(PlaylistEntity(name = "Road trip", createdAt = 0))
        dao.addSongs(id, listOf(10, 20, 30))
        var entries = dao.entries(id)
        assertEquals(listOf(10L, 20L, 30L), entries.map { it.songId })

        dao.reorder(id, listOf(entries[2].id, entries[0].id, entries[1].id))
        entries = dao.entries(id)
        assertEquals(listOf(30L, 10L, 20L), entries.map { it.songId })

        dao.deleteEntry(entries[0].id)
        assertEquals(2, dao.entries(id).size)
        assertEquals(2, dao.observePlaylists().first().single().songCount)

        dao.delete(id)
        assertEquals(0, dao.entries(id).size) // foreign key cascade
    }

    @Test fun favouritesAndHistory() = runBlocking {
        db.favouriteDao().add(com.wanyamajohn.musicplayer.data.db.FavouriteEntity(5, 1))
        assertEquals(listOf(5L), db.favouriteDao().observeIds().first())
        db.favouriteDao().remove(5)
        assertEquals(emptyList<Long>(), db.favouriteDao().observeIds().first())

        val h = db.historyDao()
        h.insert(com.wanyamajohn.musicplayer.data.db.PlaybackHistoryEntity(songId = 1, playedAt = 1))
        h.insert(com.wanyamajohn.musicplayer.data.db.PlaybackHistoryEntity(songId = 2, playedAt = 2))
        h.insert(com.wanyamajohn.musicplayer.data.db.PlaybackHistoryEntity(songId = 2, playedAt = 3))
        assertEquals(listOf(2L, 1L), h.observeMostPlayed().first())
        assertEquals(listOf(2L, 1L), h.observeRecent().first())
    }
}
