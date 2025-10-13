package com.demo.core.logger

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

internal class LogFileManagerImpl(
    private val config: LogConfig
) : ILogFileManager {

    private val fileDateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    override fun shareRecentLogs(context: Context, days: Int) {
        val logsDir = config.logDir ?: return
        val now = System.currentTimeMillis()
        val deadline = now - days * 24 * 3600 * 1000L

        val recentLogs = logsDir.listFiles { f ->
            f.name.startsWith("tracker-") && f.lastModified() in deadline..now
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        shareLogsAsZip(context, recentLogs, "logs-report", "App Daily Logs")
    }

    private fun shareLogsAsZip(
        context: Context,
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
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, zipFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                setType("application/zip")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Logs via"))
        }
    }

    override fun flushSync() {
        Timber.forest()
            .filterIsInstance<FileTree>()
            .firstOrNull()
            ?.flushSync()
    }


}