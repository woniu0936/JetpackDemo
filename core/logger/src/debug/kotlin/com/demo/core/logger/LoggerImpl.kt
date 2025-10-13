package com.demo.core.logger

import timber.log.Timber

/**
 * `LoggerImpl` 是 [ILogger] 接口在调试（debug）环境下的实现。
 * 它利用 [Timber] 库来分发和记录日志，确保日志能够被 Timber 配置的各种 [Timber.Tree] 接收和处理。
 * 此实现仅在调试（debug）版本中可用。
 *
 * @property tag 用于此日志器实例的日志标签。
 *
 * @see ILogger
 * @see Timber
 * @see LoggerFactory
 */
internal class LoggerImpl(private val tag: String) : ILogger {
    /**
     * 记录详细（Verbose）级别的日志信息。
     * 日志通过 [Timber.tag] 方法设置标签后，由 [Timber.v] 进行分发。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * // 内部使用示例：
     * val loggerImpl = LoggerImpl("MyDebugTag")
     * loggerImpl.v { "调试模式下的详细信息。" }
     * ```
     */
    override fun v(message: () -> String) = Timber.tag(tag).v(message())

    /**
     * 记录调试（Debug）级别的日志信息。
     * 日志通过 [Timber.tag] 方法设置标签后，由 [Timber.d] 进行分发。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * // 内部使用示例：
     * val loggerImpl = LoggerImpl("MyDebugTag")
     * loggerImpl.d { "调试模式下的调试信息。" }
     * ```
     */
    override fun d(message: () -> String) = Timber.tag(tag).d(message())

    /**
     * 记录信息（Info）级别的日志信息。
     * 日志通过 [Timber.tag] 方法设置标签后，由 [Timber.i] 进行分发。
     *
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * // 内部使用示例：
     * val loggerImpl = LoggerImpl("MyDebugTag")
     * loggerImpl.i { "调试模式下的信息。" }
     * ```
     */
    override fun i(message: () -> String) = Timber.tag(tag).i(message())

    /**
     * 记录警告（Warning）级别的日志信息。
     * 日志通过 [Timber.tag] 方法设置标签后，由 [Timber.w] 进行分发。
     *
     * @param throwable 可选的 [Throwable] 对象，表示导致警告的异常或错误。
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * // 内部使用示例：
     * val loggerImpl = LoggerImpl("MyDebugTag")
     * loggerImpl.w { "调试模式下的警告信息。" }
     * try {
     *     throw IllegalStateException("测试警告")
     * } catch (e: Exception) {
     *     loggerImpl.w(e) { "调试模式下的带异常警告。" }
     * }
     * ```
     */
    override fun w(throwable: Throwable?, message: () -> String) = Timber.tag(tag).w(throwable, message())

    /**
     * 记录错误（Error）级别的日志信息。
     * 日志通过 [Timber.tag] 方法设置标签后，由 [Timber.e] 进行分发。
     *
     * @param throwable 可选的 [Throwable] 对象，表示导致错误的异常或错误。
     * @param message 一个 lambda 表达式，返回要记录的日志字符串。
     *
     * @example
     * ```kotlin
     * // 内部使用示例：
     * val loggerImpl = LoggerImpl("MyDebugTag")
     * loggerImpl.e { "调试模式下的错误信息。" }
     * try {
     *     throw RuntimeException("测试错误")
     * } catch (e: Exception) {
     *     loggerImpl.e(e) { "调试模式下的带异常错误。" }
     * }
     * ```
     */
    override fun e(throwable: Throwable?, message: () -> String) = Timber.tag(tag).e(throwable, message())
}