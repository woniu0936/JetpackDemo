package com.demo.core.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.system.exitProcess

/**
 * `CrashActivity` 是一个用户友好的 Activity，用于在调试（DEBUG）版本中显示崩溃信息。
 * 当应用程序发生未捕获异常时，此 Activity 会被启动，展示堆栈跟踪信息，并提供重启应用、复制日志和分享报告的功能。
 * 此 Activity 仅在调试（debug）版本中启用。
 *
 * @see CrashHandlerImpl
 */
class CrashActivity : AppCompatActivity() {

    companion object {
        /**
         * 用于在 Intent 中传递堆栈跟踪信息的键。
         */
        const val EXTRA_STACK_TRACE = "extra_stack_trace"

        /**
         * 创建一个用于启动 [CrashActivity] 的 Intent。
         *
         * @param context 上下文。
         * @param stackTrace 要显示的堆栈跟踪信息字符串。
         * @return 配置好的 Intent 实例。
         *
         * @example
         * ```kotlin
         * // 在 CrashHandlerImpl 中使用
         * val intent = CrashActivity.createIntent(context, "完整的堆栈跟踪信息")
         * context.startActivity(intent)
         * ```
         */
        internal fun createIntent(context: Context, stackTrace: String): Intent {
            return Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_STACK_TRACE, stackTrace)
            }
        }
    }

    /**
     * Activity 的创建生命周期回调。
     * 初始化布局，显示堆栈跟踪，并设置按钮的点击监听器。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace available."

        val errorDetailsTextView: TextView = findViewById(R.id.tv_error_details)
        errorDetailsTextView.text = formatStackTrace(stackTrace)

        val restartButton: Button = findViewById(R.id.btn_restart_app)
        restartButton.setOnClickListener {
            restartApp()
        }

        val copyButton: Button = findViewById(R.id.btn_copy_log)
        copyButton.setOnClickListener {
            copyToClipboard(stackTrace)
        }

        val shareButton: Button = findViewById(R.id.btn_share_report)
        shareButton.setOnClickListener {
            // CrashManager 的 shareCrashReport 是 debug-only 的扩展函数
            CrashHandlerImpl.shareCrashReport(this)
        }
    }

    /**
     * 格式化堆栈跟踪信息，以高亮显示关键信息，提高可读性。
     *
     * @param stackTrace 原始的堆栈跟踪信息字符串。
     * @return 格式化后的 [SpannableStringBuilder] 对象。
     */
    private fun formatStackTrace(stackTrace: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder(stackTrace)
        try {
            // Highlight the exception class name (e.g., java.lang.RuntimeException)
            val exceptionPattern = """\b[\w\.]*Exception\b""".toRegex()
            exceptionPattern.findAll(stackTrace).forEach { matchResult ->
                builder.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.color_exception)), // Define this color
                    matchResult.range.first,
                    matchResult.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    matchResult.range.first,
                    matchResult.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Highlight the "at com.yourcompany..." lines (your app's code)
            val yourPackageName = packageName.substringBeforeLast(".") // Adjust if needed
            val packagePattern = """at $yourPackageName\..*""".toRegex()
            packagePattern.findAll(stackTrace).forEach { matchResult ->
                builder.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.color_app_stack)), // Define this color
                    matchResult.range.first,
                    matchResult.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } catch (e: Exception) {
            // Formatting is best-effort. If it fails, just return the raw text.
        }
        return builder
    }

    /**
     * 将给定的文本复制到剪贴板。
     *
     * @param text 要复制的文本内容。
     *
     * @example
     * ```kotlin
     * copyToClipboard("Hello World")
     * ```
     */
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    /**
     * 重启应用程序。
     * 此方法会创建一个新的任务栈并启动应用的启动 Activity，然后终止当前进程。
     *
     * @example
     * ```kotlin
     * restartApp()
     * ```
     */
    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
        startActivity(mainIntent)
        exitProcess(0)
    }

    /**
     * 处理后退按钮的按下事件。
     * 为了防止用户返回到崩溃前的状态，此方法会重启应用程序。
     */
    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent user from going back to the broken app state
        restartApp()
    }
}