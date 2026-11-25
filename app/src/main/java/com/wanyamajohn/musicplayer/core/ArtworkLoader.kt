package com.wanyamajohn.musicplayer.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkCache {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 12).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val missing = HashSet<String>()
    @Synchronized fun get(key: String): Bitmap? = cache.get(key)
    @Synchronized fun put(key: String, b: Bitmap) { cache.put(key, b) }
    @Synchronized fun isMissing(key: String) = key in missing
    @Synchronized fun markMissing(key: String) { missing.add(key) }
}

/** Decodes artwork down-sampled to ~[targetPx]; results (and misses) are cached in memory. */
suspend fun loadArtwork(context: Context, uriString: String, targetPx: Int = 512): Bitmap? {
    ArtworkCache.get(uriString)?.let { return it }
    if (ArtworkCache.isMissing(uriString)) return null
    val bmp = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
                ?: return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }
    if (bmp != null) ArtworkCache.put(uriString, bmp) else ArtworkCache.markMissing(uriString)
    return bmp
}

suspend fun loadVideoThumbnail(context: Context, uriString: String): Bitmap? {
    if (Build.VERSION.SDK_INT < 29) return null
    val key = "v:$uriString"
    ArtworkCache.get(key)?.let { return it }
    val b = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.loadThumbnail(Uri.parse(uriString), Size(320, 180), null) }.getOrNull()
    }
    if (b != null) ArtworkCache.put(key, b)
    return b
}
