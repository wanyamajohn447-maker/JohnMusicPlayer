package com.wanyamajohn.musicplayer.core

/** 215_000 ms -> "3:35"; 3_725_000 ms -> "1:02:05". Negative values are treated as 0. */
fun formatDuration(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
