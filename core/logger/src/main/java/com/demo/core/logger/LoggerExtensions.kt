package com.demo.core.logger

fun Any.logger(): Logger {
    return LogManager.getLogger(this.javaClass)
}

// 顶层函数的 v, d, i 变得更简单
fun logV(tag: String, message: () -> String) = LogManager.getLogger(tag).v(message)
fun logD(tag: String, message: () -> String) = LogManager.getLogger(tag).d(message)
fun logI(tag: String, message: () -> String) = LogManager.getLogger(tag).i(message)

/**
 * [For Top-Level & Lambdas]
 * A top-level function for logging a WARNING message.
 */
fun logW(tag: String, throwable: Throwable? = null, message: () -> String) {
    if (throwable != null) {
        LogManager.getLogger(tag).w(throwable, message)
    } else {
        LogManager.getLogger(tag).w(message)
    }
}

/**
 * [For Top-Level & Lambdas]
 * A top-level function for logging an ERROR message.
 */
fun logE(tag: String, throwable: Throwable? = null, message: () -> String) {
    if (throwable != null) {
        LogManager.getLogger(tag).e(throwable, message)
    } else {
        LogManager.getLogger(tag).e(message)
    }
}
