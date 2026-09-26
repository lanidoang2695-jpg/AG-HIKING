package com.example.data.tile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.data.model.MapLayerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.*

class MapTileEngine(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // 24MB memory cache for active tiles
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private val tilesBaseDir: File
        get() = File(context.filesDir, "map_tiles").apply { if (!exists()) mkdirs() }

    companion object {
        fun lon2tile(lon: Double, zoom: Int): Int {
            return floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
        }

        fun lat2tile(lat: Double, zoom: Int): Int {
            val latRad = Math.toRadians(lat)
            return floor((1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
        }

        fun tile2lon(x: Int, zoom: Int): Double {
            return x.toDouble() / (1 shl zoom) * 360.0 - 180.0
        }

        fun tile2lat(y: Int, zoom: Int): Double {
            val n = Math.PI - 2.0 * Math.PI * y.toDouble() / (1 shl zoom)
            return Math.toDegrees(atan(sinh(n)))
        }

        fun estimateTilesCount(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, minZ: Int, maxZ: Int): Int {
            var count = 0
            for (z in minZ..maxZ) {
                val minX = lon2tile(minLon, z)
                val maxX = lon2tile(maxLon, z)
                val minY = lat2tile(maxLat, z)
                val maxY = lat2tile(minLat, z)
                val xCount = (maxX - minX + 1).coerceAtLeast(1)
                val yCount = (maxY - minY + 1).coerceAtLeast(1)
                count += xCount * yCount
            }
            return count
        }
    }

    private fun getTileFile(layer: MapLayerType, z: Int, x: Int, y: Int): File {
        val dir = File(tilesBaseDir, "${layer.name}/$z/$x")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$y.png")
    }

    suspend fun getTileBitmap(layer: MapLayerType, z: Int, x: Int, y: Int): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${layer.name}_${z}_${x}_${y}"

        // 1. Check memory cache
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // 2. Check local disk cache
        val localFile = getTileFile(layer, z, x, y)
        if (localFile.exists() && localFile.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(localFile.absolutePath)
                if (bmp != null) {
                    memoryCache.put(cacheKey, bmp)
                    return@withContext bmp
                }
            } catch (_: Exception) {}
        }

        // 3. Fetch from network
        val url = layer.urlTemplate
            .replace("{z}", z.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AG-HIKING-Pro-Outdoor/1.0 (Android; Offline-First)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    // Save to local disk
                    FileOutputStream(localFile).use { it.write(bytes) }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        memoryCache.put(cacheKey, bmp)
                        return@withContext bmp
                    }
                }
            }
        } catch (_: Exception) {
            // Offline fallback: purely graceful
        }

        return@withContext null
    }

    suspend fun downloadRegion(
        layer: MapLayerType,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        minZoom: Int,
        maxZoom: Int,
        onProgress: (downloaded: Int, total: Int, bytes: Long) -> Unit,
        isCancelled: () -> Boolean
    ): Long = withContext(Dispatchers.IO) {
        val total = estimateTilesCount(minLat, maxLat, minLon, maxLon, minZoom, maxZoom)
        var downloaded = 0
        var totalBytes = 0L

        for (z in minZoom..maxZoom) {
            val minX = lon2tile(minLon, z)
            val maxX = lon2tile(maxLon, z)
            val minY = lat2tile(maxLat, z)
            val maxY = lat2tile(minLat, z)

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    if (isCancelled()) return@withContext totalBytes

                    val localFile = getTileFile(layer, z, x, y)
                    if (localFile.exists() && localFile.length() > 0) {
                        downloaded++
                        totalBytes += localFile.length()
                    } else {
                        val bmp = getTileBitmap(layer, z, x, y)
                        downloaded++
                        if (localFile.exists()) {
                            totalBytes += localFile.length()
                        }
                    }
                    onProgress(downloaded, total, totalBytes)
                }
            }
        }
        totalBytes
    }

    fun deleteLayerCache(layer: MapLayerType) {
        val dir = File(tilesBaseDir, layer.name)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        memoryCache.evictAll()
    }

    fun clearAllTilesCache(): Long {
        val size = calculateTilesStorageBytes()
        if (tilesBaseDir.exists()) {
            tilesBaseDir.deleteRecursively()
            tilesBaseDir.mkdirs()
        }
        memoryCache.evictAll()
        return size
    }

    fun calculateTilesStorageBytes(): Long {
        fun dirSize(file: File): Long {
            if (!file.exists()) return 0L
            if (!file.isDirectory) return file.length()
            var sum = 0L
            file.listFiles()?.forEach { sum += dirSize(it) }
            return sum
        }
        return dirSize(tilesBaseDir)
    }
}
