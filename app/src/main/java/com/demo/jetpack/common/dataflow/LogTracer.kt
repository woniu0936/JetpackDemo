package com.demo.jetpack.common.dataflow

import com.demo.jetpack.core.extension.logD
import java.util.UUID
import com.demo.jetpack.BuildConfig

object LogTracer {

    const val TAG = "CacheFirst"

    // 生成一个唯一的请求ID，用于日志追踪，方便关联同一操作的不同日志条目。
    fun newId(): String = UUID.randomUUID().toString().takeLast(8)

    // 用于测量代码块执行时间的内联函数，并记录执行耗时、线程信息和调用位置。
    inline fun <T> trace(requestId: String, block: () -> T): T {
        if (BuildConfig.DEBUG) { // Only execute in debug builds
            val start = System.nanoTime()          // 使用单调时钟，不受系统时间调整影响，确保测量准确性。
            return block().also {
                val costNs = System.nanoTime() - start
                val costMs = costNs / 1_000_000.0  // 将纳秒转换为毫秒，并保留三位小数。
                logD(TAG) { "[$requestId] ${Thread.currentThread().name} | ${callerLocation()} | cost=${costMs}ms | result=$it" }
            }
        }
        return block() // In release builds, just execute the block without tracing
    }

    // 获取调用 `trace` 方法的代码位置（文件名、行号和方法名），用于日志中定位代码。
    fun callerLocation(): String {
        if (BuildConfig.DEBUG) { // Only execute in debug builds
            val st = Thread.currentThread().stackTrace
            var i = 0
            // 遍历堆栈跟踪，找到第一个非 `trace` 和 `invoke` 的方法，即为实际的调用者。
            while (i < st.size) {
                val method = st[i].methodName
                if (method != "trace" && method != "invoke") {
                    return "${st[i].fileName}:${st[i].lineNumber} (${st[i].methodName})"
                }
                i++
            }
            return "unknown"
        }
        return "unknown" // In release builds, return "unknown"
    }
}