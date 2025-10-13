package com.demo.core.logger

fun Any.logger(): Logger {
    return AppLogger.getLogger(this.javaClass)
}

// 顶层函数的 v, d, i 变得更简单
fun logV(tag: String, message: () -> String) = AppLogger.getLogger(tag).v(message)
fun logD(tag: String, message: () -> String) = AppLogger.getLogger(tag).d(message)
fun logI(tag: String, message: () -> String) = AppLogger.getLogger(tag).i(message)

/**
 * [For Top-Level & Lambdas]
 * A top-level function for logging a WARNING message.
 */
fun logW(tag: String, throwable: Throwable? = null, message: () -> String) {
    if (throwable != null) {
        AppLogger.getLogger(tag).w(throwable, message)
    } else {
        AppLogger.getLogger(tag).w(message)
    }
}

/**
 * [For Top-Level & Lambdas]
 * A top-level function for logging an ERROR message.
 */
fun logE(tag: String, throwable: Throwable? = null, message: () -> String) {
    if (throwable != null) {
        AppLogger.getLogger(tag).e(throwable, message)
    } else {
        AppLogger.getLogger(tag).e(message)
    }
}
