package com.demo.core.crash

/**
 * 崩溃处理系统的配置类。
 * 在 Kotlin 中使用 `CrashManager.init { ... }` 或在 Java 中使用 `new CrashConfig.Builder()` 来构建实例。
 *
 * @property onCrashCallback 当发生未捕获异常时执行的回调函数。此回调接收一个 [Throwable] 参数。
 *
 * @see CrashManager
 */
class CrashConfig private constructor(
    val onCrashCallback: ((throwable: Throwable) -> Unit)?
) {
    /**
     * 用于构建 [CrashConfig] 实例的 Builder 模式，对 Kotlin 和 Java 都很友好。
     *
     * @example
     * ```kotlin
     * // Kotlin 示例：使用 Builder 配置崩溃回调
     * val config = CrashConfig.Builder()
     *     .onCrash { throwable ->
     *         // 在这里处理崩溃，例如上传日志、报告给崩溃收集服务
     *         println("应用程序崩溃了: ${throwable.message}")
     *     }
     *     .build()
     * CrashManager.init(applicationContext, config)
     * ```
     *
     * @example
     * ```java
     * // Java 示例：使用 Builder 配置崩溃回调
     * CrashConfig config = new CrashConfig.Builder()
     *     .onCrash(throwable -> {
     *         // 在这里处理崩溃
     *         System.out.println("应用程序崩溃了: " + throwable.getMessage());
     *         return Unit.INSTANCE; // Kotlin lambda 在 Java 中需要返回 Unit.INSTANCE
     *     })
     *     .build();
     * CrashManager.init(getApplicationContext(), config);
     * ```
     */
    class Builder {
        private var onCrashCallback: ((throwable: Throwable) -> Unit)? = null

        /**
         * 设置当发生未捕获异常时要执行的回调函数。
         * 这是用于在崩溃时刷新日志或向第三方服务报告错误的主要机制。
         *
         * @param callback 一个 lambda 表达式，接收导致崩溃的 [Throwable] 对象。
         * @return Builder 实例，支持链式调用。
         *
         * @example
         * ```kotlin
         * builder.onCrash { throwable ->
         *     // 处理崩溃逻辑
         *     Log.e("Crash", "应用崩溃", throwable)
         * }
         * ```
         */
        fun onCrash(callback: (throwable: Throwable) -> Unit) = apply { this.onCrashCallback = callback }

        /**
         * 构建并返回不可变的 [CrashConfig] 实例。
         *
         * @return 配置好的 [CrashConfig] 实例。
         */
        fun build() = CrashConfig(onCrashCallback)
    }
}