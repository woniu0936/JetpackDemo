package com.demo.core.logger

import java.util.UUID

/**
 * 用于跟踪日志的标签常量。
 * 这是一个内部使用的常量，用于标识跟踪相关的日志。
 */
@PublishedApi
internal const val TRACE_LOG_TAG = "Trace"

/**
 * [调试专用] 内部实现，用于测量代码块的执行时间并记录跟踪日志。
 * 此函数仅在调试（debug）版本中启用。
 *
 * @param tag 跟踪日志的特定标签，用于区分不同的跟踪点。
 * @param block 要执行并测量其耗时的代码块。
 * @return 代码块的执行结果。
 *
 * @example
 * ```kotlin
 * // 示例：测量一个函数的执行时间
 * fun fetchData(): String {
 *     return traceInternal("FetchData") {
 *         Thread.sleep(100) // 模拟耗时操作
 *         "Data fetched"
 *     }
 * }
 *
 * // 日志输出示例：
 * // D/Trace: [Trace:FetchData] main | MyClass.kt:25 (fetchData) | cost=100.123ms | result=Data fetched
 * ```
 */
@PublishedApi
internal inline fun <T> traceInternal(tag: String, block: () -> T): T {
    val start = System.nanoTime()
    return block().also { result ->
        val costNs = System.nanoTime() - start
        val costMs = costNs / 1_000_000.0
        // 现在可以安全地访问 TRACE_LOG_TAG 和 callerLocation
        AppLogger.getLogger(TRACE_LOG_TAG).d {
            "[Trace:$tag] ${Thread.currentThread().name} | ${callerLocation()} | cost=%.3fms | result=$result".format(costMs)
        }
    }
}

/**
 * [调试专用] 内部实现，用于生成一个新的跟踪 ID。
 * 此函数仅在调试（debug）版本中启用。
 *
 * @return 一个新的、简短的 UUID 字符串作为跟踪 ID。
 *
 * @example
 * ```kotlin
 * val traceId = newTraceIdInternal()
 * println("生成的跟踪 ID: $traceId") // 例如: 生成的跟踪 ID: a1b2c3d4
 * ```
 */
@PublishedApi
internal fun newTraceIdInternal(): String {
    return UUID.randomUUID().toString().substring(0, 8)
}

/**
 * [调试专用] 内部辅助函数，用于查找调用者的位置（文件名、行号和方法名）。
 * 此函数仅在调试（debug）版本中启用。
 *
 * @return 包含调用者位置信息的字符串，格式为 "文件名:行号 (方法名)"，如果无法确定则返回 "unknown"。
 *
 * @example
 * ```kotlin
 * // 内部使用示例：
 * val location = callerLocation()
 * println("调用者位置: $location") // 例如: 调用者位置: MyClass.kt:42 (myFunction)
 * ```
 */
@PublishedApi
internal fun callerLocation(): String {
    val stackTrace = Thread.currentThread().stackTrace
    // 通常，栈帧 0 是 Thread.getStackTrace()
    // 栈帧 1 是 callerLocation()
    // 栈帧 2 是 traceInternal() 或 newTraceIdInternal()
    // 栈帧 3 是调用 traceInternal() 或 newTraceIdInternal() 的地方
    // 栈帧 4 是实际的业务逻辑调用者
    val caller = stackTrace.getOrNull(4)
    return if (caller != null) {
        "${caller.fileName}:${caller.lineNumber} (${caller.methodName})"
    } else {
        "unknown"
    }
}