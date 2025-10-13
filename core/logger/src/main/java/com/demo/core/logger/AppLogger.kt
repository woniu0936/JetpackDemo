package com.demo.core.logger

import android.content.Context
import java.io.File

/**
 * [公共门面] 整个日志模块的单一公共入口点。
 *
 * `AppLogger` 是一个单例对象，负责初始化日志系统、提供日志配置选项以及获取日志器实例。
 * 必须在应用程序启动时调用其 `init` 方法进行初始化。
 *
 * @see LogConfig
 * @see Logger
 * @see LoggerFactory
 */
object AppLogger {

    @Volatile
    private var isInitialized = false
    private val factory = LoggerFactory
    private val lock = Any()

    private lateinit var fileManager: ILogFileManager

    /**
     * [推荐用于 Kotlin - DSL 风格]
     * 使用 DSL 风格的配置块初始化日志系统。
     * 此方法必须且只能在 `Application.onCreate()` 中调用一次。
     *
     * @param context 应用程序上下文。
     * @param block 一个 lambda 表达式，用于配置 [LogConfig.Builder]。
     *
     * @example
     * ```kotlin
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         AppLogger.init(this) {
     *             enableFileLogging(true) // 启用文件日志
     *             retentionDays(7) // 日志文件保留7天
     *             logDir(File(filesDir, "my_app_logs")) // 指定日志存储目录
     *         }
     *     }
     * }
     * ```
     */
    fun init(context: Context, block: LogConfig.Builder.() -> Unit) {
        val builder = LogConfig.Builder()
        builder.block() // Apply the user's DSL configuration to the builder
        performInitialization(context, builder.build())
    }

    /**
     * 使用给定的配置初始化日志系统。
     * 此方法必须且只能在 `Application.onCreate()` 中调用一次。
     *
     * @param context 应用程序上下文。
     * @param config [LogConfig] 实例。可以使用 `LogConfig.Builder()` 创建一个。
     *
     * @example
     * ```kotlin
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         val config = LogConfig.Builder()
     *             .enableFileLogging(true)
     *             .retentionDays(10)
     *             .build()
     *         AppLogger.init(this, config)
     *     }
     * }
     * ```
     */
    @JvmStatic
    fun init(context: Context, config: LogConfig) {
        performInitialization(context, config)
    }

    /**
     * 使用默认配置初始化日志系统。
     * 此方法必须且只能在 `Application.onCreate()` 中调用一次。
     *
     * 默认配置通常包括禁用文件日志，或使用默认的日志保留天数和存储目录。
     *
     * @param context 应用程序上下文。
     *
     * @example
     * ```kotlin
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         AppLogger.init(this) // 使用默认配置初始化
     *     }
     * }
     * ```
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
                    .retentionDays(config.retentionDays)
                    .logDir(File(context.filesDir, "logs")) // <-- 设置默认值
                    .build()
            } else {
                config
            }

            initializeTrees(finalConfig)

            fileManager = LogFileManagerImpl(finalConfig)

            isInitialized = true
        }
    }

    // --- 日志文件管理 API ---

    /**
     * 将最近几天的日志文件打包成 ZIP 文件并提供分享。
     * 在发布（release）版本中，此操作将不执行任何功能（no-op）。
     *
     * @param context 用于启动分享意图的上下文。
     * @param days 要包含的最近日志天数，默认为1天。
     *
     * @example
     * ```kotlin
     * // 示例：分享最近一天的日志
     * AppLogger.shareRecentLogs(this, 1)
     *
     * // 示例：分享最近三天的日志
     * AppLogger.shareRecentLogs(this, 3)
     * ```
     */
    @JvmStatic
    fun shareRecentLogs(context: Context, days: Int = 1) {
        checkInitialized()
        fileManager.shareRecentLogs(context, days)
    }

    /**
     * 同步地将所有缓冲的日志写入磁盘。
     * 在发布（release）版本中，此操作将不执行任何功能（no-op）。
     *
     * 此方法会阻塞当前线程直到所有日志写入完成，应谨慎使用以避免阻塞 UI 线程。
     *
     * @example
     * ```kotlin
     * // 示例：在应用程序退出前强制写入所有日志
     * AppLogger.flushSync()
     * ```
     */
    fun flushSync() {
        if (!isInitialized) return // Don't throw if not initialized, just ignore
        fileManager.flushSync()
    }

    /**
     * 根据给定的标签获取一个 [Logger] 实例。
     *
     * @param tag 用于标识日志来源的字符串标签。
     * @return 对应标签的 [Logger] 实例。
     *
     * @throws IllegalStateException 如果 `AppLogger` 未初始化。
     *
     * @example
     * ```kotlin
     * // 示例：获取一个带有特定标签的 Logger 实例
     * val myLogger = AppLogger.getLogger("MyFeature")
     * myLogger.d { "这是一条来自 MyFeature 的调试信息。" }
     * ```
     */
    @JvmStatic
    fun getLogger(tag: String): Logger {
        checkInitialized()
        return factory.getLogger(tag)
    }

    /**
     * 使用类的简单名称获取一个 [Logger] 实例。
     *
     * @param clazz 要获取其日志器的类对象。
     * @return 对应类的 [Logger] 实例。
     *
     * @throws IllegalStateException 如果 `AppLogger` 未初始化。
     *
     * @example
     * ```kotlin
     * // 示例：获取一个基于类名的 Logger 实例
     * class MyService {
     *     private val logger = AppLogger.getLogger(MyService::class.java)
     *     fun doWork() {
     *         logger.i { "MyService 正在执行工作。" }
     *     }
     * }
     * ```
     */
    @JvmStatic
    fun getLogger(clazz: Class<*>): Logger {
        checkInitialized()
        return factory.getLogger(clazz)
    }

    /**
     * 一个方便的扩展函数，用于为任何 Kotlin 类获取 [Logger] 实例。
     * 推荐在 Kotlin 中使用此方法，通过 `by lazy` 委托属性来延迟初始化日志器。
     *
     * @receiver 任何 Kotlin 对象。
     * @return 对应接收者类的 [Logger] 实例。
     *
     * @throws IllegalStateException 如果 `AppLogger` 未初始化。
     *
     * @example
     * ```kotlin
     * // 示例：在 Kotlin 类中使用扩展函数获取 Logger 实例
     * class MyViewModel : ViewModel() {
     *     private val log by lazy { logger() }
     *
     *     fun loadData() {
     *         log.d { "正在加载数据..." }
     *     }
     * }
     * ```
     */
    fun Any.logger(): Logger {
        checkInitialized()
        return factory.getLogger(this.javaClass)
    }

    private fun checkInitialized() {
        if (!isInitialized && BuildConfig.DEBUG) {
            throw IllegalStateException(
                "AppLogger.init(context) must be called before obtaining a logger. " +
                        "Typically, this is done in your Application.onCreate()."
            )
        }
    }
}