package com.wanyamajohn.musicplayer.core

data class LrcLine(val timeMs: Long, val text: String)

object LrcParser {
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    /** Parses LRC text. Lines without time tags (ID tags such as [ar:...]) are ignored. */
    fun parse(text: String): List<LrcLine> {
        val out = ArrayList<LrcLine>()
        for (raw in text.lineSequence()) {
            val matches = tag.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val content = raw.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                out += LrcLine(min * 60_000 + sec * 1000 + ms, content)
            }
        }
        return out.sortedBy { it.timeMs }
    }

    fun isSynced(text: String): Boolean = parse(text).isNotEmpty()

    /** Index of the line active at [positionMs], or -1 before the first line. */
    fun currentIndex(lines: List<LrcLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.size - 1
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) { ans = mid; lo = mid + 1 } else hi = mid - 1
        }
        return ans
    }
}
