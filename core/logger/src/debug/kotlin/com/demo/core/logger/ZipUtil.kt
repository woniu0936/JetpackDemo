package com.demo.core.logger

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * `ZipUtil` 是一个内部工具类，用于将多个文件压缩成一个 ZIP 文件。
 * 此工具类主要用于日志模块中，方便地打包和分享日志文件。
 * 此实现仅在调试（debug）版本中可用。
 */
internal object ZipUtil {

    private const val BUFFER_SIZE = 8192 // 8 KB buffer

    /**
     * 将给定的文件列表压缩成一个 ZIP 文件。
     *
     * @param files 要压缩的 [File] 对象列表。
     * @param outZip 输出的 ZIP 文件对象。
     * @return 如果压缩成功则返回 `true`，否则返回 `false`。
     *
     * @example
     * ```kotlin
     * // 示例：将两个日志文件压缩成一个 ZIP 文件
     * val logFile1 = File(context.filesDir, "logs/tracker-2023-10-26.log")
     * val logFile2 = File(context.filesDir, "logs/tracker-2023-10-25.log")
     * val outputZipFile = File(context.cacheDir, "my_logs.zip")
     *
     * if (logFile1.exists() && logFile2.exists()) {
     *     val success = ZipUtil.zipFiles(listOf(logFile1, logFile2), outputZipFile)
     *     if (success) {
     *         println("日志文件已成功压缩到: ${outputZipFile.absolutePath}")
     *     } else {
     *         println("日志文件压缩失败。")
     *     }
     * } else {
     *     println("部分日志文件不存在。")
     * }
     * ```
     */
    fun zipFiles(files: List<File>, outZip: File): Boolean {
        if (files.isEmpty()) {
            Log.w("ZipUtil", "No files to zip.")
            return false
        }

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outZip))).use { zos ->
                for (file in files) {
                    if (!file.exists() || !file.canRead()) {
                        Log.w("ZipUtil", "Skipping unreadable or non-existent file: ${file.name}")
                        continue
                    }

                    val entry = ZipEntry(file.name)
                    zos.putNextEntry(entry)

                    BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { bis ->
                        bis.copyTo(zos, BUFFER_SIZE)
                    }

                    zos.closeEntry()
                }
                zos.flush()
            }
            return true
        } catch (e: Exception) {
            Log.e("ZipUtil", "Failed to create zip file using java.util.zip", e)
            outZip.delete()
            return false
        }
    }
}