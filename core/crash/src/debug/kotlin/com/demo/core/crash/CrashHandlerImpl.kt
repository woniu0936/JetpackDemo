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
 * [调试专用] [ICrashHandler] 的调试（debug）版本实现。
 * 它负责保存崩溃报告、执行自定义回调，然后启动一个用户界面 Activity 来显示崩溃信息。
 * 此实现仅在调试（debug）版本中启用。
 *
 * @property context 应用程序上下文。
 * @property config 崩溃配置对象，包含崩溃回调等信息。
 *
 * @see ICrashHandler
 * @see CrashManager
 * @see CrashActivity
 */
internal class CrashHandlerImpl(
    private val context: Context,
    private val config: CrashConfig
) : ICrashHandler {

    companion object {
        private const val CRASH_DIR = "crashes"
        private val CRASH_FILE_FORMAT = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

        /**
         * 获取崩溃报告存储的目录。
         *
         * @param context 应用程序上下文。
         * @return 存储崩溃报告的 [File] 目录对象。
         *
         * @example
         * ```kotlin
         * val crashDir = CrashHandlerImpl.getCrashLogDir(applicationContext)
         * println("崩溃报告目录: ${crashDir.absolutePath}")
         * ```
         */
        fun getCrashLogDir(context: Context): File {
            return File(context.filesDir, CRASH_DIR)
        }

        /**
         * 创建崩溃报告的文件名。
         *
         * @return 格式为 "crash-yyyyMMdd-HHmmss.txt" 的文件名字符串。
         *
         * @example
         * ```kotlin
         * val fileName = CrashHandlerImpl.createCrashReportFileName()
         * println("新的崩溃报告文件名: $fileName") // 例如: crash-20231027-103045.txt
         * ```
         */
        fun createCrashReportFileName(): String {
            return "crash-${CRASH_FILE_FORMAT.format(Date())}.txt"
        }

        /**
         * 查找最新的崩溃报告，创建一个分享意图，并启动它。
         * 此方法通常由 [CrashActivity] 调用，用于让用户分享崩溃报告。
         *
         * @param context 应用程序上下文。
         *
         * @example
         * ```kotlin
         * // 在 CrashActivity 中点击分享按钮时调用
         * CrashHandlerImpl.shareCrashReport(this) // `this` 是 CrashActivity 实例
         * ```
         */
        fun shareCrashReport(context: Context) {
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

    /**
     * 处理未捕获的异常。
     * 这是 [Thread.UncaughtExceptionHandler] 接口的实现，当任何线程发生未捕获异常时会被调用。
     * 它会保存崩溃报告，执行配置的回调，然后启动 [CrashActivity] 来显示崩溃信息。
     *
     * @param thread 发生未捕获异常的线程。
     * @param throwable 发生的 [Throwable] 异常对象。
     *
     * @example
     * ```kotlin
     * // 此方法由 Android 系统在发生未捕获异常时自动调用，无需手动调用。
     * // 通常在 CrashManager.init 中设置：
     * // Thread.setDefaultUncaughtExceptionHandler(CrashHandlerImpl(context, config))
     * ```
     */
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
     * 将详细的崩溃报告保存到文件系统。
     * 报告中包含应用信息、设备信息和完整的堆栈跟踪。
     *
     * @param throwable 发生的 [Throwable] 异常对象。
     * @return 完整的堆栈跟踪字符串。
     */
    private fun saveCrashReportToFile(throwable: Throwable): String {
        val stackTraceString = Log.getStackTraceString(throwable)
        val reportFile = File(getCrashLogDir(context), createCrashReportFileName())

        try {
            val versionName = AppInfoUtils.getVersionName(context)
            val versionCode = AppInfoUtils.getVersionCode(context)
            reportFile.parentFile?.mkdirs()
            reportFile.sink().buffer().use { sink ->
                sink.writeUtf8("--- App & Device Info ---\\n")
                sink.writeUtf8("Time: ${Date()}\\n")
                sink.writeUtf8("App Version: $versionName ($versionCode)\\n")
                sink.writeUtf8("OS Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\\n")
                sink.writeUtf8("Device: ${Build.MANUFACTURER} ${Build.MODEL}\\n\\n")
                sink.writeUtf8("--- Stack Trace ---\\n")
                sink.writeUtf8(stackTraceString)
            }
            Log.i("CrashHandler", "Crash report saved to: ${reportFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to save crash report to file", e)
        }
        return stackTraceString
    }
}