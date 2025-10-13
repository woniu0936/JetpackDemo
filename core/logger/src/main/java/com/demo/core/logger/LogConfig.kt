package com.demo.core.logger

import java.io.File

/**
 * 日志系统的配置类。
 * 使用嵌套的 [Builder] 类来构建 [LogConfig] 实例。
 *
 * 此类一旦构建，即为不可变（immutable）对象，有助于提升线程安全性。
 *
 * @property enableFileLogging 是否启用文件日志记录。如果为 `true`，日志将被写入文件系统。
 * @property logDir 日志文件存储的目录。如果为 `null`，系统将使用默认目录（通常是应用私有目录下的 `logs` 文件夹）。
 * @property retentionDays 日志文件保留的天数。超过此天数的日志文件将被删除。
 *
 * @see AppLogger.init
 */
class LogConfig private constructor( // <-- 构造函数设为 private
    val enableFileLogging: Boolean,
    val logDir: File?,
    val retentionDays: Int
) {
    /**
     * 用于构建 [LogConfig] 实例的经典 Builder 模式。
     * 它提供了一个流畅的、可链式调用的 API，对 Kotlin 和 Java 都很友好。
     *
     * @example
     * ```kotlin
     * // 示例：使用 Builder 构建 LogConfig
     * val config = LogConfig.Builder()
     *     .enableFileLogging(true) // 启用文件日志
     *     .logDir(File(context.filesDir, "my_app_logs")) // 设置日志目录
     *     .retentionDays(14) // 设置日志保留天数
     *     .build()
     *
     * AppLogger.init(context, config)
     * ```
     */
    class Builder {
        private var enableFileLogging: Boolean = true
        private var logDir: File? = null
        private var retentionDays: Int = 7

        /**
         * 设置是否启用文件日志记录。
         *
         * @param enable `true` 表示启用文件日志，`false` 表示禁用。
         * @return Builder 实例，支持链式调用。
         */
        fun enableFileLogging(enable: Boolean) = apply { this.enableFileLogging = enable }

        /**
         * 设置日志文件的自定义存储目录。
         *
         * @param directory 用于存储日志文件的 [File] 对象。
         * @return Builder 实例，支持链式调用。
         */
        fun logDir(directory: File) = apply { this.logDir = directory }

        /**
         * 设置日志文件保留的天数。
         * 超过此天数的日志文件将在清理时被删除。
         *
         * @param days 日志文件保留的天数。
         * @return Builder 实例，支持链式调用。
         */
        fun retentionDays(days: Int) = apply { this.retentionDays = days }

        /**
         * 构建并返回不可变的 [LogConfig] 实例。
         *
         * @return 配置好的 [LogConfig] 实例。
         */
        fun build() = LogConfig(
            enableFileLogging = enableFileLogging,
            logDir = logDir,
            retentionDays = retentionDays
        )
    }
}