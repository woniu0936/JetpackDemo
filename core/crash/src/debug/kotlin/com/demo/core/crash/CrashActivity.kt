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
 * A user-friendly activity to display crash information in DEBUG builds.
 */
class CrashActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"

        internal fun createIntent(context: Context, stackTrace: String): Intent {
            return Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_STACK_TRACE, stackTrace)
            }
        }
    }

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
     * Formats the stack trace to highlight key information for better readability.
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

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
        startActivity(mainIntent)
        exitProcess(0)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent user from going back to the broken app state
        restartApp()
    }
}