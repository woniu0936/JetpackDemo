package com.demo.core.logger

/**
 * The core logging interface.
 * `Throwable` is intentionally restricted to `w` and `e` levels to enforce
 * semantic correctness in logging practices.
 */
internal interface ILogger {
    fun v(message: () -> String)
    fun d(message: () -> String)
    fun i(message: () -> String)
    fun w(throwable: Throwable? = null, message: () -> String)
    fun e(throwable: Throwable? = null, message: () -> String)
}