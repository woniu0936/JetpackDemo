package com.demo.core.logger

import com.demo.core.logger.ILogger
import android.content.Context

internal class LoggerImpl(private val tag: String) : ILogger {
    override fun v(message: () -> String) { /* No-op */ }
    override fun d(message: () -> String) { /* No-op */ }
    override fun i(message: () -> String) { /* No-op */ }
    override fun w(throwable: Throwable?, message: () -> String) { /* No-op */ }
    override fun e(throwable: Throwable?, message: () -> String) { /* No-op */ }
}

