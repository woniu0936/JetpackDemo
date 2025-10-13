package com.demo.core.logger

import com.demo.core.logger.ILogger

internal object LoggerImpl : ILogger {
    override fun init(context: android.content.Context) { /* no-op */ }
    override fun d(tag: String, msg: () -> String) { /* no-op */ }
    override fun i(tag: String, msg: () -> String) { /* no-op */ }
    override fun w(tag: String, msg: () -> String, tr: Throwable?) { /* no-op */ }
    override fun e(tag: String, msg: () -> String, tr: Throwable?) { /* no-op */ }
}