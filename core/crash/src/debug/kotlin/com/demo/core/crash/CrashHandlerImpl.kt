package com.demo.core.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import okio.buffer
import okio.sink
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess
/**
 * DEBUG implementation of [ICrashHandler].
 * It saves the crash report, executes callbacks, and then launches a UI activity.
 */
internal class CrashHandlerImpl(
    private val context: Context,
    private val config: CrashConfig
) : ICrashHandler {

    companion object {
        private const val CRASH_DIR = "crashes"
        private val CRASH_FILE_FORMAT = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

        /** Gets the directory where crash reports are stored. */
        fun getCrashLogDir(context: Context): File {
            return File(context.filesDir, CRASH_DIR)
        }

        /** Creates a filename for the crash report. */
        fun createCrashReportFileName(): String {
            return "crash-${CRASH_FILE_FORMAT.format(Date())}.txt"
        }

        /**
         * Finds the latest crash report, creates a share intent, and starts it.
         */
        fun shareReport(context: Context) {
            val crashDir = getCrashLogDir(context)
            if (!crashDir.exists() || !crashDir.isDirectory) {
                Log.w("CrashSharer", "Crash directory does not exist.")
                // You might want to show a Toast here in a real app
                return
            }

            // Find the most recent crash report file
            val latestCrashFile = crashDir.listFiles { file -> file.name.startsWith("crash-") }
                ?.maxByOrNull { it.lastModified() }

            if (latestCrashFile == null) {
                Log.w("CrashSharer", "No crash reports found to share.")
                // You might want to show a Toast here
                return
            }

            // The authority must match what's declared in the AndroidManifest
            val authority = "${context.packageName}.fileprovider"

            try {
                val uri = FileProvider.getUriForFile(context, authority, latestCrashFile)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain" // Use text/plain for .txt files
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "App Crash Report - ${latestCrashFile.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Share Crash Report via")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Use NEW_TASK if called from a non-Activity context
                context.startActivity(chooserIntent)

            } catch (e: IllegalArgumentException) {
                Log.e(
                    "CrashSharer",
                    "Failed to get URI for file. Did you forget to declare the FileProvider in your AndroidManifest.xml?",
                    e
                )
                // You might want to show a Toast with an error message here
            }
        }
    }

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 1. Save crash report to a file first. This is the most critical step.
            val stackTrace = saveCrashReportToFile(throwable)

            // 2. Execute external callbacks (e.g., logger.flushSync())
            config.onCrashCallback?.invoke(throwable)

            // 3. Launch the crash activity to show UI
            context.startActivity(CrashActivity.createIntent(context, stackTrace))

        } catch (e: Exception) {
            Log.e("CrashHandler", "Error while handling crash", e)
        } finally {
            // 4. Chain to the default handler and kill the process
            defaultHandler?.uncaughtException(thread, throwable)
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    /**
     * Saves the detailed crash report to a file using Okio.
     * @return The full stack trace string.
     */
    private fun saveCrashReportToFile(throwable: Throwable): String {
        val stackTraceString = Log.getStackTraceString(throwable)
        val reportFile = File(getCrashLogDir(context), createCrashReportFileName())

        try {
            val versionName = AppInfoUtils.getVersionName(context)
            val versionCode = AppInfoUtils.getVersionCode(context)
            reportFile.parentFile?.mkdirs()
            reportFile.sink().buffer().use { sink ->
                sink.writeUtf8("--- App & Device Info ---\n")
                sink.writeUtf8("Time: ${Date()}\n")
                sink.writeUtf8("App Version: $versionName ($versionCode)\n")
                sink.writeUtf8("OS Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                sink.writeUtf8("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n\n")
                sink.writeUtf8("--- Stack Trace ---\n")
                sink.writeUtf8(stackTraceString)
            }
            Log.i("CrashHandler", "Crash report saved to: ${reportFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to save crash report to file", e)
        }
        return stackTraceString
    }
}

