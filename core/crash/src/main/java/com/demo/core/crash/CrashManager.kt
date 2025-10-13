package com.demo.core.crash

import android.content.Context

/**
 * [公共门面] 崩溃处理模块的单一公共入口点。
 *
 * `CrashManager` 是一个单例对象，负责初始化崩溃处理系统。
 * 必须在应用程序启动时调用其 `init` 方法进行初始化，以捕获未处理的异常。
 *
 * @see CrashConfig
 * @see ICrashHandler
 */
object CrashManager {

    @Volatile
    private var isInitialized = false
    private val lock = Any()

    /**
     * 初始化崩溃处理系统。此方法必须且只能在 `Application.onCreate()` 中调用一次。
     * 推荐使用 Kotlin 的 DSL 风格配置。
     *
     * @param context 应用程序上下文。
     * @param block 一个 lambda 表达式，用于配置 [CrashConfig.Builder]。
     *
     * @example
     * ```kotlin
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         CrashManager.init(this) {
     *             onCrash { throwable ->
     *                 // 在这里处理崩溃，例如上传日志、报告给崩溃收集服务
     *                 println("应用程序崩溃了: ${throwable.message}")
     *             }
     *         }
     *     }
     * }
     * ```
     */
    fun init(context: Context, block: CrashConfig.Builder.() -> Unit = {}) {
        val config = CrashConfig.Builder().apply(block).build()
        performInitialization(context, config)
    }

    /**
     * [适用于 Java] 使用 [CrashConfig] 对象初始化崩溃处理系统。
     * 此方法必须且只能在 `Application.onCreate()` 中调用一次。
     *
     * @param context 应用程序上下文。
     * @param config [CrashConfig] 实例。可以使用 `CrashConfig.Builder()` 创建一个。
     *
     * @example
     * ```java
     * public class MyApplication extends Application {
     *     @Override
     *     public void onCreate() {
     *         super.onCreate();
     *         CrashConfig config = new CrashConfig.Builder()
     *             .onCrash(throwable -> {
     *                 System.out.println("App crashed: " + throwable.getMessage());
     *                 return Unit.INSTANCE; // Kotlin lambda 在 Java 中需要返回 Unit.INSTANCE
     *             })
     *             .build();
     *         CrashManager.init(this, config);
     *     }
     * }
     * ```
     */
    @JvmStatic
    fun init(context: Context, config: CrashConfig) {
        performInitialization(context, config)
    }

    /**
     * [简化版] 使用默认配置初始化崩溃处理系统。
     * 此方法必须且只能在 `Application.onCreate()` 中调用一次。
     *
     * @param context 应用程序上下文。
     *
     * @example
     * ```kotlin
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         CrashManager.init(this) // 使用默认配置初始化
     *     }
     * }
     * ```
     */
    @JvmStatic
    fun init(context: Context) {
        performInitialization(context, CrashConfig.Builder().build())
    }

    private fun performInitialization(context: Context, config: CrashConfig) {
        synchronized(lock) {
            if (isInitialized) return
            // The magic happens here: The compiler links to the correct
            // CrashHandlerImpl class based on the current build variant.
            val handler = CrashHandlerImpl(context.applicationContext, config)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            isInitialized = true
        }
    }
    
}