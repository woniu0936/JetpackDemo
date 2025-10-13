package com.demo.core.logger

import android.content.Context
import java.io.File

/**
 * [Public Facade] The single, public entry point for the entire logging module.
 */
object LogManager {

    @Volatile
    private var isInitialized = false
    private val factory = LoggerFactory
    private val lock = Any()

    private lateinit var fileManager: ILogFileManager

    /**
     * [For Kotlin - Recommended DSL]
     * Initializes the logging system using a DSL-style configuration block.
     * Must be called once.
     */
    fun init(context: Context, block: LogConfig.Builder.() -> Unit) {
        val builder = LogConfig.Builder()
        builder.block() // Apply the user's DSL configuration to the builder
        performInitialization(context, builder.build())
    }

    /**
     * Initializes the logging system with a given configuration.
     * Must be called once in `Application.onCreate()`.
     *
     * @param context The application context.
     * @param config The [LogConfig] instance. Use `LogConfig.Builder()` to create one.
     */
    @JvmStatic
    fun init(context: Context, config: LogConfig) {
        performInitialization(context, config)
    }

    /**
     * Initializes the logging system with default configuration.
     * Must be called once.
     */
    @JvmStatic
    fun init(context: Context) {
        // 调用上面的 init，传入一个默认构造的 config
        performInitialization(context, LogConfig.Builder().build())
    }

    private fun performInitialization(context: Context, config: LogConfig) {
        synchronized(lock) {
            if (isInitialized) return

            // 【关键】: 在这里处理默认 logDir，而不是在 Builder 中
            val finalConfig = if (config.logDir == null) {
                // 如果用户没有提供，我们创建一个新的包含默认目录的 config
                // (虽然 LogConfig 是不可变的，但我们可以创建一个新的)
                LogConfig.Builder()
                    .enableFileLogging(config.enableFileLogging)
                    .enableCrashReporting(config.enableCrashReporting)
                    .retentionDays(config.retentionDays)
                    .logDir(File(context.filesDir, "logs")) // <-- 设置默认值
                    .build()
            } else {
                config
            }

            LoggerInitializer.initialize(context.applicationContext, finalConfig)

            fileManager = LogFileManagerImpl(config)

            isInitialized = true
        }
    }

    // --- Log File Management API ---

    /**
     * Shares log files from recent days as a ZIP file.
     * This is a no-op in release builds.
     * @param context The context to start the share intent.
     * @param days The number of recent days' logs to include.
     */
    @JvmStatic
    fun shareRecentLogs(context: Context, days: Int = 1) {
        checkInitialized()
        fileManager.shareRecentLogs(context, days)
    }

    /**
     * Shares the latest crash report along with recent log files.
     * This is a no-op in release builds.
     * @param context The context to start the share intent.
     * @param days The number of recent days' logs to include with the crash report.
     */
    @JvmStatic
    fun shareCrashReport(context: Context, days: Int = 3) {
        checkInitialized()
        fileManager.shareCrashReport(context, days)
    }

    /**
     * Synchronously flushes all buffered logs to disk.
     * This is a no-op in release builds.
     */
    fun flushSync() {
        if (!isInitialized) return // Don't throw if not initialized, just ignore
        fileManager.flushSync()
    }

    /**
     * Gets a [Logger] instance for the given tag.
     */
    @JvmStatic
    fun getLogger(tag: String): Logger {
        checkInitialized()
        return factory.getLogger(tag)
    }

    /**
     * Gets a [Logger] instance using the simple name of a class.
     */
    @JvmStatic
    fun getLogger(clazz: Class<*>): Logger {
        checkInitialized()
        return factory.getLogger(clazz)
    }

    /**
     * A convenience extension to get a [Logger] instance for any class.
     * Usage in Kotlin: `private val log by lazy { LogManager.logger() }`
     */
    fun Any.logger(): Logger {
        checkInitialized()
        return factory.getLogger(this.javaClass)
    }

    private fun checkInitialized() {
        if (!isInitialized && BuildConfig.DEBUG) {
            throw IllegalStateException(
                "LogManager.init(context) must be called before obtaining a logger. " +
                        "Typically, this is done in your Application.onCreate()."
            )
        }
    }
}