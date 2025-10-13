package com.demo.core.logger

/**
 * 为任意 [Any] 对象提供一个方便的扩展函数，用于获取与其类名关联的 [Logger] 实例。
 * 推荐在 Kotlin 中使用此方法，通过 `by lazy` 委托属性来延迟初始化日志器。
 *
 * @receiver 任何 Kotlin 对象。
 * @return 对应接收者类的 [Logger] 实例。
 *
 * @throws IllegalStateException 如果 `AppLogger` 未初始化。
 *
 * @example
 * ```kotlin
 * class MyPresenter {
 *     private val log by lazy { logger() } // 延迟初始化，只在首次使用时创建
 *
 *     fun presentData() {
 *         log.i { "Presenter 正在准备数据。" }
 *     }
 * }
 * ```
 */
fun Any.logger(): Logger {
    return AppLogger.getLogger(this.javaClass)
}

/**
 * 记录详细（Verbose）级别的顶层日志信息。
 * 适用于不需要特定 `Logger` 实例，或在 lambda 表达式中直接记录日志的场景。
 *
 * @param tag 用于标识日志来源的字符串标签。
 * @param message 一个 lambda 表达式，返回要记录的日志字符串。
 *
 * @example
 * ```kotlin
 * // 示例：记录一个顶层详细日志
 * logV("Network", { "请求 URL: $url" })
 * ```
 */
fun logV(tag: String, message: () -> String) = AppLogger.getLogger(tag).v(message)

/**
 * 记录调试（Debug）级别的顶层日志信息。
 * 适用于不需要特定 `Logger` 实例，或在 lambda 表达式中直接记录日志的场景。
 *
 * @param tag 用于标识日志来源的字符串标签。
 * @param message 一个 lambda 表达式，返回要记录的日志字符串。
 *
 * @example
 * ```kotlin
 * // 示例：记录一个顶层调试日志
 * logD("Database", { "查询结果: $count 条记录" })
 * ```
 */
fun logD(tag: String, message: () -> String) = AppLogger.getLogger(tag).d(message)

/**
 * 记录信息（Info）级别的顶层日志信息。
 * 适用于不需要特定 `Logger` 实例，或在 lambda 表达式中直接记录日志的场景。
 *
 * @param tag 用于标识日志来源的字符串标签。
 * @param message 一个 lambda 表达式，返回要记录的日志字符串。
 *
 * @example
 * ```kotlin
 * // 示例：记录一个顶层信息日志
 * logI("Lifecycle", { "Activity 'MainActivity' 已创建。" })
 * ```
 */
fun logI(tag: String, message: () -> String) = AppLogger.getLogger(tag).i(message)

/**
 * 记录警告（Warning）级别的顶层日志信息。
 * 适用于不需要特定 `Logger` 实例，或在 lambda 表达式中直接记录日志的场景。
 * 支持可选的 [Throwable] 参数。
 *
 * @param tag 用于标识日志来源的字符串标签。
 * @param throwable 可选的 [Throwable] 对象，表示导致警告的异常或错误。
 * @param message 一个 lambda 表达式，返回要记录的日志字符串。
 *
 * @example
 * ```kotlin
 * // 示例：记录一个不带 Throwable 的顶层警告日志
 * logW("Config", { "API Key 未配置，使用默认值。" })
 *
 * // 示例：记录一个带 Throwable 的顶层警告日志
 * try {
 *     // ...
 * } catch (e: SomeWarningException) {
 *     logW("Parser", e, { "解析数据时遇到小问题。" })
 * }
 * ```
 */
fun logW(tag: String, throwable: Throwable? = null, message: () -> String) {
    if (throwable != null) {
        AppLogger.getLogger(tag).w(throwable, message)
    } else {
        AppLogger.getLogger(tag).w(message)
    }
}

/**
 * 记录错误（Error）级别的顶层日志信息。
 * 适用于不需要特定 `Logger` 实例，或在 lambda 表达式中直接记录日志的场景。
 * 支持可选的 [Throwable] 参数。
 *
 * @param tag 用于标识日志来源的字符串标签。
 * @param throwable 可选的 [Throwable] 对象，表示导致错误的异常或错误。
 * @param message 一个 lambda 表达式，返回要记录的日志字符串。
 *
 * @example
 * ```kotlin
 * // 示例：记录一个不带 Throwable 的顶层错误日志
 * logE("Critical", { "数据库连接失败，应用无法启动。" })
 *
 * // 示例：记录一个带 Throwable 的顶层错误日志
 * try {
 *     // ...
 * } catch (e: CriticalErrorException) {
 *     logE("Crash", e, { "应用程序发生致命错误。" })
 * }
 * ```
 */
fun logE(tag: String, throwable: Throwable? = null, message: () -> String) {
    if (throwable != null) {
        AppLogger.getLogger(tag).e(throwable, message)
    } else {
        AppLogger.getLogger(tag).e(message)
    }
}

/**
 * [调试专用] [顶层函数 & Lambda] 跟踪给定代码块的执行时间并记录结果。
 * 必须提供一个描述操作的标签。
 *
 * 在发布（release）版本中，此函数将简单地执行代码块，不产生任何性能开销。
 *
 * @param tag 跟踪操作的描述性标签。
 * @param block 要执行并测量其耗时的代码块。
 * @return 代码块的执行结果。
 *
 * @example
 * ```kotlin
 * // 示例：测量一个耗时操作的执行时间
 * fun someUtility() {
 *     val result = trace("HeavyCalculation") {
 *         // ... 执行耗时操作 ...
 *         "计算完成"
 *     }
 *     // result 将是 "计算完成"
 * }
 *
 * // 示例：在顶层函数中使用
 * val processedData = trace("DataProcessing") {
 *     // ... 数据处理逻辑 ...
 *     "处理后的数据"
 * }
 * ```
 */
inline fun <T> trace(tag: String, block: () -> T): T {
    // 此调用是内联的。在发布版本中，它会直接调用空的 `traceInternal`，
    // 然后 `traceInternal` 也会被内联，最终只执行 `block`。
    return AppLogger.trace(tag, block)
}