package com.demo.core.logger

/**
 * 一个绑定到特定标签的日志器实例。提供对 Kotlin 和 Java 都友好的 API。
 * 通过 `AppLogger.getLogger(...)` 获取实例。
 *
 * 此类封装了实际的日志记录实现 [ILogger]，并提供了不同日志级别的方法。
 *
 * @property loggerImpl 实际执行日志记录操作的 [ILogger] 实现。
 *
 * @see AppLogger.getLogger
 * @see ILogger
 */
class Logger internal constructor(private val loggerImpl: ILogger) {

    // --- Kotlin API (基于 Lambda 表达式) ---
    /**
     * 记录详细（Verbose）级别的日志信息。
     * 推荐在 Kotlin 中使用此方法，因为它利用了 `noinline` lambda 表达式，可以避免不必要的字符串构建开销。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * val logger = AppLogger.getLogger("MyTag")
     * logger.v { "用户ID: ${userId} 正在执行操作。" }
     * ```
     */
    fun v(message: () -> String) = loggerImpl.v(message)

    /**
     * 记录调试（Debug）级别的日志信息。
     * 推荐在 Kotlin 中使用此方法。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * val logger = AppLogger.getLogger("MyTag")
     * logger.d { "数据加载完成，耗时 ${timeInMs}ms。" }
     * ```
     */
    fun d(message: () -> String) = loggerImpl.d(message)

    /**
     * 记录信息（Info）级别的日志信息。
     * 推荐在 Kotlin 中使用此方法。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * val logger = AppLogger.getLogger("MyTag")
     * logger.i { "应用程序启动成功。" }
     * ```
     */
    fun i(message: () -> String) = loggerImpl.i(message)

    /**
     * 记录警告（Warning）级别的日志信息，不带 [Throwable]。
     * 推荐在 Kotlin 中使用此方法。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * val logger = AppLogger.getLogger("MyTag")
     * logger.w { "配置项 'featureX' 未设置，使用默认值。" }
     * ```
     */
    fun w(message: () -> String) = loggerImpl.w(null, message)

    /**
     * 记录警告（Warning）级别的日志信息，带有一个 [Throwable]。
     * 推荐在 Kotlin 中使用此方法。
     *
     * @param throwable 导致警告的 [Throwable] 对象。
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * val logger = AppLogger.getLogger("MyTag")
     * try {
     *     // 某些可能抛出异常的操作
     * } catch (e: IOException) {
     *     logger.w(e) { "文件读取操作失败，但程序可以继续。" }
     * }
     * ```
     */
    fun w(throwable: Throwable, message: () -> String) = loggerImpl.w(throwable, message)

    /**
     * 记录错误（Error）级别的日志信息，不带 [Throwable]。
     * 推荐在 Kotlin 中使用此方法。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * val logger = AppLogger.getLogger("MyTag")
     * logger.e { "关键服务启动失败。" }
     * ```
     */
    fun e(message: () -> String) = loggerImpl.e(null, message)

    /**
     * 记录错误（Error）级别的日志信息，带有一个 [Throwable]。
     * 推荐在 Kotlin 中使用此方法。
     *
     * @param throwable 导致错误的 [Throwable] 对象。
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * val logger = AppLogger.getLogger("MyTag")
     * try {
     *     // 某些可能抛出严重异常的操作
     * } catch (e: Exception) {
     *     logger.e(e) { "应用程序遇到一个致命错误。" }
     * }
     * ```
     */
    fun e(throwable: Throwable, message: () -> String) = loggerImpl.e(throwable, message)

    // --- Java API (基于 String) ---
    /**
     * 记录详细（Verbose）级别的日志信息，适用于 Java 调用。
     *
     * @param message 要记录的日志字符串。
     *
     * @example
     * ```java
     * Logger logger = AppLogger.getLogger("MyJavaTag");
     * logger.v("这是一个来自 Java 的详细日志。");
     * ```
     */
    fun v(message: String) = loggerImpl.v { message }

    /**
     * 记录调试（Debug）级别的日志信息，适用于 Java 调用。
     *
     * @param message 要记录的日志字符串。
     *
     * @example
     * ```java
     * Logger logger = AppLogger.getLogger("MyJavaTag");
     * logger.d("调试信息: 变量X = " + xValue);
     * ```
     */
    fun d(message: String) = loggerImpl.d { message }

    /**
     * 记录信息（Info）级别的日志信息，适用于 Java 调用。
     *
     * @param message 要记录的日志字符串。
     *
     * @example
     * ```java
     * Logger logger = AppLogger.getLogger("MyJavaTag");
     * logger.i("用户 'John Doe' 成功登录。");
     * ```
     */
    fun i(message: String) = loggerImpl.i { message }

    /**
     * 记录警告（Warning）级别的日志信息，适用于 Java 调用。
     * 支持可选的 [Throwable] 参数。
     *
     * @param message 要记录的日志字符串。
     * @param throwable 可选的 [Throwable] 对象，表示导致警告的异常或错误。
     *
     * @example
     * ```java
     * Logger logger = AppLogger.getLogger("MyJavaTag");
     * logger.w("数据解析失败，但已使用默认值。");
     * try {
     *     // ...
     * } catch (Exception e) {
     *     logger.w("处理数据时发生警告", e);
     * }
     * ```
     */
    @JvmOverloads fun w(message: String, throwable: Throwable? = null) = loggerImpl.w(throwable) { message }

    /**
     * 记录错误（Error）级别的日志信息，适用于 Java 调用。
     * 支持可选的 [Throwable] 参数。
     *
     * @param message 要记录的日志字符串。
     * @param throwable 可选的 [Throwable] 对象，表示导致错误的异常或错误。
     *
     * @example
     * ```java
     * Logger logger = AppLogger.getLogger("MyJavaTag");
     * logger.e("应用程序初始化失败。");
     * try {
     *     // ...
     * } catch (Exception e) {
     *     logger.e("发生严重错误", e);
     * }
     * ```
     */
    @JvmOverloads fun e(message: String, throwable: Throwable? = null) = loggerImpl.e(throwable) { message }
}