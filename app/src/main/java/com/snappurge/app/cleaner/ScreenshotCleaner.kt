package com.snappurge.app.cleaner

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

data class ScreenshotItem(
    val file: File,
    val sizeBytes: Long,
    val lastModified: Long
)

data class CleanResult(
    val scannedCount: Int,
    val matchedCount: Int,
    val deletedCount: Int,
    val freedBytes: Long
)

object ScreenshotCleaner {
    private const val TAG = "ScreenshotCleaner"

    /**
     * 获取常见安卓厂商（华为、荣耀、小米、OPPO、vivo、原生等）的截图存储目录
     */
    fun getScreenshotDirectories(): List<File> {
        val storageRoot = Environment.getExternalStorageDirectory()
        val dirs = mutableListOf<File>()

        // 1. 标准 Pictures/Screenshots (荣耀/华为/OPPO等常见)
        dirs.add(File(storageRoot, "Pictures/Screenshots"))
        // 2. 标准 DCIM/Screenshots (小米/Redmi/vivo/三星等常见)
        dirs.add(File(storageRoot, "DCIM/Screenshots"))
        // 3. 屏幕录制目录 (小米MIUI/HyperOS, 常见录屏应用)
        dirs.add(File(storageRoot, "DCIM/ScreenRecorder"))
        dirs.add(File(storageRoot, "Movies/ScreenRecorder"))
        dirs.add(File(storageRoot, "Movies/Screenrecords"))
        dirs.add(File(storageRoot, "Pictures/ScreenRecorder"))
        // 4. 部分第三方与原生常见
        dirs.add(File(storageRoot, "Pictures/ScreenCapture"))
        dirs.add(File(storageRoot, "DCIM/ScreenCapture"))

        return dirs.filter { it.exists() && it.isDirectory }
    }

    /**
     * 获取用户在系统图库中标记为“收藏/星标”的图片与视频路径，防止误删用户珍藏
     */
    private fun getFavoriteImagePaths(context: Context): Set<String> {
        val favorites = mutableSetOf<String>()
        val uris = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        for (uri in uris) {
            try {
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                val selection = "${MediaStore.MediaColumns.IS_FAVORITE} = 1"
                context.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    null,
                    null
                )?.use { cursor ->
                    val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataIndex)
                        if (path != null) {
                            favorites.add(path)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Query favorites failed: ${e.message}")
            }
        }
        return favorites
    }

    /**
     * 扫描符合前缀的截图文件（支持预览）
     * @param olderThanHours 超出多少小时；若 <= 0 则无视保留时长，扫描所有匹配项
     */
    fun scan(
        context: Context,
        prefixes: Set<String>,
        olderThanHours: Int = 0
    ): List<ScreenshotItem> {
        val cutoff = if (olderThanHours <= 0) {
            Long.MAX_VALUE
        } else {
            System.currentTimeMillis() - (olderThanHours.toLong() * 60 * 60 * 1000L)
        }
        val favorites = getFavoriteImagePaths(context)
        val matchedItems = mutableListOf<ScreenshotItem>()

        val dirs = getScreenshotDirectories()
        for (dir in dirs) {
            val files = dir.listFiles() ?: continue
            for (file in files) {
                if (!file.isFile) continue

                // 排除图库中标记为收藏的文件
                if (favorites.contains(file.absolutePath)) continue

                // 检查是否匹配任意自定义前缀 (忽略大小写)
                val matchesPrefix = prefixes.isEmpty() || prefixes.any { prefix ->
                    file.name.startsWith(prefix.trim(), ignoreCase = true)
                }

                if (matchesPrefix) {
                    if (olderThanHours <= 0 || file.lastModified() <= cutoff) {
                        matchedItems.add(
                            ScreenshotItem(
                                file = file,
                                sizeBytes = file.length(),
                                lastModified = file.lastModified()
                            )
                        )
                    }
                }
            }
        }

        return matchedItems
    }

    /**
     * 执行物理删除并同步刷新 MediaStore 相册
     */
    fun clean(context: Context, items: List<ScreenshotItem>): CleanResult {
        var deletedCount = 0
        var freedBytes = 0L
        val deletedPaths = mutableListOf<String>()

        for (item in items) {
            try {
                val size = item.sizeBytes
                val path = item.file.absolutePath
                if (item.file.delete()) {
                    deletedCount++
                    freedBytes += size
                    deletedPaths.add(path)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Delete file failed: ${item.file.name}", e)
            }
        }

        // 通知系统图库移除已删除的缩略图，避免出现灰色坏块
        if (deletedPaths.isNotEmpty()) {
            MediaScannerConnection.scanFile(
                context,
                deletedPaths.toTypedArray(),
                null
            ) { path, uri ->
                Log.d(TAG, "MediaScanner scanned: $path, uri: $uri")
            }
        }

        return CleanResult(
            scannedCount = items.size,
            matchedCount = items.size,
            deletedCount = deletedCount,
            freedBytes = freedBytes
        )
    }
}
