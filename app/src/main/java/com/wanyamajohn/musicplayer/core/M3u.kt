package com.wanyamajohn.musicplayer.core

import com.wanyamajohn.musicplayer.domain.model.Song

data class M3uEntry(val path: String, val title: String?, val durationSec: Long?)

object M3u {
    fun build(songs: List<Song>): String = buildString {
        appendLine("#EXTM3U")
        songs.forEach {
            appendLine("#EXTINF:${it.durationMs / 1000},${it.artist} - ${it.title}")
            appendLine((it.folder ?: "") + it.displayName)
        }
    }

    fun parse(text: String): List<M3uEntry> {
        val out = ArrayList<M3uEntry>()
        var title: String? = null
        var dur: Long? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim().removePrefix("\uFEFF")
            when {
                line.isEmpty() || line == "#EXTM3U" -> Unit
                line.startsWith("#EXTINF:") -> {
                    val body = line.removePrefix("#EXTINF:")
                    dur = body.substringBefore(',').trim().toLongOrNull()
                    title = body.substringAfter(',', "").trim().ifEmpty { null }
                }
                line.startsWith("#") -> Unit
                else -> { out += M3uEntry(line, title, dur); title = null; dur = null }
            }
        }
        return out
    }

    fun baseName(path: String): String = path.substringAfterLast('/').substringAfterLast('\\')
}
