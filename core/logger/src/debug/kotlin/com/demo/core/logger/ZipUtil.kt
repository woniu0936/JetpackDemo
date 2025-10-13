package com.demo.core.logger

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal object ZipUtil {

    private const val BUFFER_SIZE = 8192 // 8 KB buffer

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