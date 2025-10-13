package com.demo.core.logger

import android.util.Log
import okio.appendingSink
import okio.buffer
import okio.BufferedSink
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * `FileTree` 是一个 [Timber.Tree] 的实现，负责将日志写入文件系统。
 *
 * 它根据日期轮换日志文件，并定期清理过期日志。此实现仅在调试（debug）版本中启用。
 *
 * @property config 日志配置对象，包含日志目录和保留天数等信息。
 *
 * @see LogConfig
 * @see Timber
 */
internal class FileTree(
    private val config: LogConfig // <-- 接收 LogConfig 对象
) : Timber.Tree() {

    // 从 config 中获取日志目录和保留天数
    private val logsDir: File = config.logDir!! // 在 Debug 初始化时已确保非空
    private val keepDays: Int = config.retentionDays

    private val utcTimeZone = TimeZone.getTimeZone("UTC")
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utcTimeZone }
    private val logTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).apply { timeZone = utcTimeZone }

    private val singleWriter = Executors.newSingleThreadExecutor { r ->
        Thread(r, "log-writer").apply { isDaemon = true }
    }

    private val maintenanceExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "log-maintenance").apply { isDaemon = true }
    }

    @Volatile private var today: String = ""
    private var sink: BufferedSink? = null

    private var lineCount = 0
    private var lastFlushTime = 0L
    private val FLUSH_TRIGGER_COUNT = 200
    private val FLUSH_TRIGGER_MILLIS = 5000L

    init {
        logsDir.mkdirs()
    }

    /**
     * 记录日志消息到文件。
     * 此方法是 [Timber.Tree] 的核心实现，它将格式化后的日志写入当前日志文件。
     *
     * @param priority 日志的优先级（例如 [Log.VERBOSE], [Log.DEBUG]）。
     * @param tag 日志的标签，用于标识日志来源。
     * @param message 日志消息字符串。
     * @param t 伴随日志的 [Throwable] 对象，如果存在的话。
     *
     * @example
     * ```kotlin
     * // Timber 会自动调用此方法
     * Timber.d("这是一条调试日志，将写入文件。")
     * Timber.e(Exception("测试异常"), "发生了一个错误。")
     * ```
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val now = System.currentTimeMillis()
        val dateStr = fileDateFormat.format(Date(now))
        val timeStr = logTimeFormat.format(Date(now))
        val priorityChar = when (priority) {
            Log.VERBOSE -> 'V'; Log.DEBUG -> 'D'; Log.INFO -> 'I'
            Log.WARN -> 'W'; Log.ERROR -> 'E'; else -> 'A'
        }
        val line = "$priorityChar $timeStr [${tag ?: "App"}] -> $message"
        singleWriter.execute { writeLine(dateStr, line, t) }
    }

    /**
     * 将单行日志写入文件。
     * 此方法在单独的线程中执行，以避免阻塞主线程。
     *
     * @param date 当前日志的日期字符串，用于文件轮换。
     * @param line 要写入的日志行内容。
     * @param t 伴随日志的 [Throwable] 对象，如果存在的话。
     */
    private fun writeLine(date: String, line: String, t: Throwable?) {
        try {
            rotateIfNeeded(date)
            val currentSink = sink ?: run {
                Log.e("FileTree", "Log sink is null. Fallback to Logcat.")
                Log.println(Log.INFO, "LogcatFallback", line)
                return
            }
            currentSink.writeUtf8(line).writeUtf8("\n")
            t?.let { currentSink.writeUtf8(Log.getStackTraceString(it) + "\n") }

            lineCount++
            val now = System.currentTimeMillis()
            if (lineCount >= FLUSH_TRIGGER_COUNT || now - lastFlushTime >= FLUSH_TRIGGER_MILLIS) {
                currentSink.flush()
                lineCount = 0
                lastFlushTime = now
            }
        } catch (e: Exception) {
            Log.e("FileTree", "Failed to write log. Fallback to Logcat.", e)
            Log.println(Log.INFO, "LogcatFallback", line)
        }
    }

    /**
     * 检查是否需要轮换日志文件（例如，日期变更）。
     * 如果需要，关闭当前文件写入器并打开一个新的。
     *
     * @param date 当前日期字符串。
     */
    @Synchronized
    private fun rotateIfNeeded(date: String) {
        if (today == date && sink != null) return

        sink?.close()
        today = date
        val file = File(logsDir, "tracker-" + today + ".log")
        sink = try {
            file.appendingSink().buffer()
        } catch (e: Exception) {
            Log.e("FileTree", "Failed to create sink for log file: $file", e)
            null
        }
        maintenanceExecutor.execute { cleanup() }
    }

    /**
     * 清理过期的日志文件。
     * 根据 [keepDays] 配置删除早于截止日期的日志文件。
     */
    private fun cleanup() {
        val deadline = System.currentTimeMillis() - keepDays * 24 * 3600 * 1000L
        logsDir.listFiles { f -> f.name.startsWith("tracker-") }
            ?.forEach { if (it.lastModified() < deadline) it.delete() }
    }

    /**
     * 同步刷新所有缓冲的日志到磁盘。
     * 此方法会阻塞调用线程，直到日志刷新操作完成或超时。
     *
     * @example
     * ```kotlin
     * // 在应用程序退出前调用，确保所有日志都已写入文件
     * fileTree.flushSync()
     * ```
     */
    fun flushSync() {
        try {
            val future = singleWriter.submit {
                sink?.flush()
            }
            future.get(2, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            Log.w("FileTree", "Log flush timed out after 2 seconds.")
        } catch (e: Exception) {
            Log.e("FileTree", "Failed to flush logs synchronously.", e)
        }
    }
}
