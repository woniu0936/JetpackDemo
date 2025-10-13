//package com.demo.core.logger
//
//import android.os.Build
//import android.util.Log
//import okio.appendingSink
//import okio.buffer
//import java.io.File
//import java.io.PrintWriter
//import java.io.StringWriter
//import java.text.SimpleDateFormat
//import java.util.*
//
//internal class CrashFileTree(
//    private val logsDir: File
//) : Thread.UncaughtExceptionHandler {
//
//    private val utcTimeZone = TimeZone.getTimeZone("UTC")
//    private val crashTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS 'UTC'", Locale.US).apply { timeZone = utcTimeZone }
//    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utcTimeZone }
//
//    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
//
//    fun plant() {
//        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
//        if (this != defaultHandler) { // Prevent recursive planting
//            Thread.setDefaultUncaughtExceptionHandler(this)
//        }
//    }
//
//    override fun uncaughtException(t: Thread, e: Throwable) {
//        // Step 1: Immediately flush all buffered logs to disk to capture the state before the crash.
//        // This is the most critical step.
//        LogFileManager.flushSync()
//
//        // Step 2: Write the crash details to a separate, dedicated crash file.
//        writeCrashToFile(t, e)
//
//        // Step 3: Chain to the default handler to allow standard crash behavior (e.g., showing "App has stopped").
//        defaultHandler?.uncaughtException(t, e)
//    }
//
//    private fun writeCrashToFile(t: Thread, e: Throwable) {
//        val file = File(logsDir, "crash-${fileDateFormat.format(Date())}.log")
//        try {
//            file.appendingSink().buffer().use { sink ->
//                sink.writeUtf8("========== Crash @ ${crashTimeFormat.format(Date())} ==========\n")
//                sink.writeUtf8("Device  : ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE} - API ${Build.VERSION.SDK_INT})\n")
//                sink.writeUtf8("Thread  : ${t.name} (id: ${t.id})\n")
//                sink.writeUtf8("\n----- Stack Trace -----\n")
//                val sw = StringWriter()
//                e.printStackTrace(PrintWriter(sw))
//                sink.writeUtf8(sw.toString())
//                sink.writeUtf8("\n========== End Crash Report ==========\n\n")
//            }
//        } catch (ex: Exception) {
//            Log.e("CrashFileTree", "Failed to write crash log", ex)
//        }
//    }
//}