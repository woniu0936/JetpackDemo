package com.demo.core.logger

/**
 * [Release] No-op internal implementation for tracing.
 */
// 提供了同签名的 internal inline 函数，但方法体不同
@PublishedApi
internal inline fun <T> traceInternal(tag: String, block: () -> T): T {
    return block()
}

/**
 * [Release] No-op internal implementation for generating a trace ID.
 */
@PublishedApi
internal fun newTraceIdInternal(): String {
    return ""
}