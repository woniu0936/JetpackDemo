package com.demo.core.logger

/**
 * 核心日志接口。
 *
 * 此接口定义了不同日志级别的标准方法，用于在应用程序中记录信息。
 * 为了强制日志记录实践的语义正确性，[Throwable] 异常对象被有意限制在 [w] (警告) 和 [e] (错误) 级别。
 *
 * @see AppLogger
 * @see Logger
 */
internal interface ILogger {
    /**
     * 记录详细（Verbose）级别的日志信息。
     *
     * 详细日志通常用于记录所有可能有助于调试的细粒度事件。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @sample com.demo.core.logger.LoggerExtensions.v
     *
     * @example
     * ```kotlin
     * // 示例：记录一个详细日志
     * logger.v { "这是一个详细日志信息。" }
     *
     * // 示例：记录包含变量的详细日志
     * val data = "some data"
     * logger.v { "处理数据: $data" }
     * ```
     */
    fun v(message: () -> String)

    /**
     * 记录调试（Debug）级别的日志信息。
     *
     * 调试日志用于记录开发过程中有用的信息，通常在发布版本中会被移除或禁用。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @sample com.demo.core.logger.LoggerExtensions.d
     *
     * @example
     * ```kotlin
     * // 示例：记录一个调试日志
     * logger.d { "正在执行调试操作。" }
     *
     * // 示例：记录包含复杂对象的调试日志
     * val user = User("Alice", 30)
     * logger.d { "当前用户: ${user.name}, 年龄: ${user.age}" }
     * ```
     */
    fun d(message: () -> String)

    /**
     * 记录信息（Info）级别的日志信息。
     *
     * 信息日志用于记录应用程序运行过程中的重要事件，例如用户操作、系统状态变化等。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @sample com.demo.core.logger.LoggerExtensions.i
     *
     * @example
     * ```kotlin
     * // 示例：记录一个信息日志
     * logger.i { "用户成功登录。" }
     *
     * // 示例：记录包含操作结果的信息日志
     * val result = "成功"
     * logger.i { "数据加载结果: $result" }
     * ```
     */
    fun i(message: () -> String)

    /**
     * 记录警告（Warning）级别的日志信息。
     *
     * 警告日志表示可能存在问题但不会立即导致应用程序崩溃的情况。
     * 可以选择性地包含一个 [Throwable] 对象。
     *
     * @param throwable 可选的 [Throwable] 对象，表示导致警告的异常或错误。
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @sample com.demo.core.logger.LoggerExtensions.w
     *
     * @example
     * ```kotlin
     * // 示例：记录一个警告日志
     * logger.w { "配置文件未找到，使用默认设置。" }
     *
     * // 示例：记录包含异常的警告日志
     * try {
     *     // 可能会抛出异常的代码
     * } catch (e: IOException) {
     *     logger.w(e) { "文件读取失败，但应用程序可以继续运行。" }
     * }
     * ```
     */
    fun w(throwable: Throwable? = null, message: () -> String)

    /**
     * 记录错误（Error）级别的日志信息。
     *
     * 错误日志表示应用程序中发生了严重问题，可能导致功能受损或应用程序崩溃。
     * 可以选择性地包含一个 [Throwable] 对象。
     *
     * @param throwable 可选的 [Throwable] 对象，表示导致错误的异常或错误。
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @sample com.demo.core.logger.LoggerExtensions.e
     *
     * @example
     * ```kotlin
     * // 示例：记录一个错误日志
     * logger.e { "数据库连接失败，应用程序无法启动。" }
     *
     * // 示例：记录包含异常的错误日志
     * try {
     *     // 可能会抛出严重异常的代码
     * } catch (e: Exception) {
     *     logger.e(e) { "发生了一个未预期的致命错误。" }
     * }
     * ```
     */
    fun e(throwable: Throwable? = null, message: () -> String)
}