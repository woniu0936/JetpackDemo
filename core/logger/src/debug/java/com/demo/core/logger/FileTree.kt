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

internal class FileTree(
    private val logsDir: File,
    private val keepDays: Int = 7
) : Timber.Tree() {

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

    @Synchronized
    private fun rotateIfNeeded(date: String) {
        if (today == date && sink != null) return

        sink?.close()
        today = date
        val file = File(logsDir, "tracker-$today.log")
        sink = try {
            file.appendingSink().buffer()
        } catch (e: Exception) {
            Log.e("FileTree", "Failed to create sink for log file: $file", e)
            null
        }
        maintenanceExecutor.execute { cleanup() }
    }

    private fun cleanup() {
        val deadline = System.currentTimeMillis() - keepDays * 24 * 3600 * 1000L
        logsDir.listFiles { f -> f.name.startsWith("tracker-") }
            ?.forEach { if (it.lastModified() < deadline) it.delete() }
    }

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