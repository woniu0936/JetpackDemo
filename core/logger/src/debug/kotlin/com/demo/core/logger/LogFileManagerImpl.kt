package com.demo.core.logger

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * `LogFileManagerImpl` 是 [ILogFileManager] 接口的调试（debug）版本实现。
 * 它负责管理日志文件，包括分享最近的日志以及同步刷新日志到磁盘。
 * 此实现仅在调试（debug）版本中可用，不应在生产环境中使用。
 *
 * @property config 日志配置对象，包含日志目录等信息。
 *
 * @see ILogFileManager
 * @see AppLogger
 * @see FileTree
 */
internal class LogFileManagerImpl(
    private val config: LogConfig
) : ILogFileManager {

    private val fileDateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    /**
     * 收集最近指定天数的日志文件，并将其打包成 ZIP 文件，然后通过分享意图提供给用户。
     * 此方法仅在调试版本中有效，在发布版本中是一个空操作（no-op）。
     *
     * @param context 用于启动分享意图的上下文。
     * @param days 要包含的最近日志天数。
     *
     * @example
     * ```kotlin
     * // 通过 AppLogger 调用此功能
     * AppLogger.shareRecentLogs(applicationContext, 3) // 分享最近3天的日志
     * ```
     */
    override fun shareRecentLogs(context: Context, days: Int) {
        val logsDir = config.logDir ?: return
        val now = System.currentTimeMillis()
        val deadline = now - days * 24 * 3600 * 1000L

        val recentLogs = logsDir.listFiles { f ->
            f.name.startsWith("tracker-") && f.lastModified() in deadline..now
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        shareLogsAsZip(context, recentLogs, "logs-report", "App Daily Logs")
    }

    /**
     * 将指定的日志文件列表打包成 ZIP 文件，并通过 Android 的分享意图发送。
     *
     * @param context 用于启动分享意图的上下文。
     * @param filesToZip 要打包的日志文件列表。
     * @param zipFileNameBase ZIP 文件的基础名称。
     * @param subject 分享意图的主题。
     */
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

    /**
     * 同步刷新所有缓冲的日志到磁盘。
     * 此方法会查找当前活跃的 [FileTree] 实例，并调用其 `flushSync()` 方法。
     * 此方法仅在调试版本中有效，在发布版本中是一个空操作（no-op）。
     *
     * @example
     * ```kotlin
     * // 通过 AppLogger 调用此功能
     * AppLogger.flushSync()
     * ```
     */
    override fun flushSync() {
        Timber.forest()
            .filterIsInstance<FileTree>()
            .firstOrNull()
            ?.flushSync()
    }


}