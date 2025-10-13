package com.demo.core.logger

import com.demo.core.logger.ILogger
import android.content.Context

internal object LoggerImpl : ILogger {
    fun init(context: Context) { /* no-op */ }
    override fun d(tag: String, msg: () -> String) { /* no-op */ }
    override fun i(tag: String, msg: () -> String) { /* no-op */ }
    override fun w(tag: String, tr: Throwable?, msg: () -> String) { /* no-op */ }
    override fun e(tag: String, tr: Throwable?, msg: () -> String) { /* no-op */ }
}