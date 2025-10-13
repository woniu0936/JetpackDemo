package com.demo.core.crash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

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
        // You need to create a layout file, e.g., `res/layout/activity_crash.xml`
        // setContentView(R.layout.activity_crash)

        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace available."

        // val errorDetailsTextView: TextView = findViewById(R.id.tv_error_details)
        // errorDetailsTextView.text = stackTrace

        // val restartButton: Button = findViewById(R.id.btn_restart_app)
        // restartButton.setOnClickListener {
        //     // Logic to restart the app
        //     val intent = packageManager.getLaunchIntentForPackage(packageName)
        //     val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
        //     startActivity(mainIntent)
        //     Runtime.getRuntime().exit(0)
        // }

        // val copyButton: Button = findViewById(R.id.btn_copy_log)
        // copyButton.setOnClickListener {
        //     val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        //     val clip = ClipData.newPlainText("Crash Log", stackTrace)
        //     clipboard.setPrimaryClip(clip)
        //     Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        // }
    }

    /**
     * [Debug Only] Extension function to expose sharing functionality.
     * This file only exists in the 'debug' source set.
     */
    fun CrashManager.shareCrashReport(context: Context) {
        // Calls the static-like method in the companion object
        CrashHandlerImpl.shareReport(context)
    }
}