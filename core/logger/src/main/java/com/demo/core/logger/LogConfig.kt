package com.demo.core.logger

import java.io.File

/**
 * Configuration for the logging system.
 * Use the nested [Builder] class to construct an instance.
 *
 * This class is immutable once constructed, promoting thread safety.
 */
class LogConfig private constructor( // <-- 构造函数设为 private
    val enableFileLogging: Boolean,
    val logDir: File?,
    val retentionDays: Int
) {
    /**
     * A classic Builder pattern for constructing [LogConfig] instances.
     * It provides a fluent, chainable API that is friendly to both Kotlin and Java.
     */
    class Builder {
        private var enableFileLogging: Boolean = true
        private var logDir: File? = null
        private var retentionDays: Int = 7

        /** Sets whether file logging is enabled. */
        fun enableFileLogging(enable: Boolean) = apply { this.enableFileLogging = enable }

        /** Sets the custom directory for log files. */
        fun logDir(directory: File) = apply { this.logDir = directory }

        /** Sets the number of days to keep log files. */
        fun retentionDays(days: Int) = apply { this.retentionDays = days }

        /** Builds and returns the immutable [LogConfig] instance. */
        fun build() = LogConfig(
            enableFileLogging = enableFileLogging,
            logDir = logDir,
            retentionDays = retentionDays
        )
    }
}