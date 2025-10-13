package com.demo.core.logger

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object LogFileManager {

    private val fileDateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    fun logsDir(context: Context): File =
        File(context.getExternalFilesDir(null), "logs")

    fun shareRecentLogs(context: Context, authority: String, days: Int = 1) {
        val logsDir = logsDir(context)
        val now = System.currentTimeMillis()
        val deadline = now - days * 24 * 3600 * 1000L

        val recentLogs = logsDir.listFiles { f ->
            f.name.startsWith("tracker-") && f.lastModified() in deadline..now
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        shareLogsAsZip(
            context = context,
            authority = authority,
            filesToZip = recentLogs,
            zipFileNameBase = "logs-report",
            subject = "App Daily Logs"
        )
    }

    fun shareCrashReport(context: Context, authority: String, days: Int = 3) {
        val logsDir = logsDir(context)

        val crashFile = logsDir.listFiles { f -> f.name.startsWith("crash-") }
            ?.maxByOrNull { it.lastModified() }
            ?: run {
                Log.w("LogFileManager", "No crash logs found to share.")
                return
            }

        val deadline = System.currentTimeMillis() - days * 24 * 3600 * 1000L
        val recentLogs = logsDir.listFiles { f ->
            f.name.startsWith("tracker-") && f.lastModified() >= deadline
        }?.toList() ?: emptyList()

        val filesToZip = (recentLogs + crashFile).distinct()

        shareLogsAsZip(
            context = context,
            authority = authority,
            filesToZip = filesToZip,
            zipFileNameBase = "crash-report",
            subject = "App Crash Report"
        )
    }

    private fun shareLogsAsZip(
        context: Context,
        authority: String,
        filesToZip: List<File>,
        zipFileNameBase: String,
        subject: String
    ) {
        if (filesToZip.isEmpty()) {
            Log.w("LogFileManager", "No log files found to share for subject: $subject")
            return
        }

        val zipFile = File(context.cacheDir, "$zipFileNameBase-${fileDateFormat.format(Date())}.zip")

        if (ZipUtil.zipFiles(filesToZip, zipFile)) {
            val uri = FileProvider.getUriForFile(context, authority, zipFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Logs via"))
        }
    }

    fun flushSync() {
        Timber.forest()
            .filterIsInstance<FileTree>()
            .firstOrNull()
            ?.flushSync()
    }
}