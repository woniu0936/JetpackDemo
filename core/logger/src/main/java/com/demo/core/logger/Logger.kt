package com.demo.core.logger

/**
 * An instance of a logger tied to a specific tag. Provides both Kotlin and Java-friendly APIs.
 * Obtain instances via `LogManager.getLogger(...)`.
 */
class Logger internal constructor(private val loggerImpl: ILogger) {

    // --- Kotlin API (Lambda-based) ---
    fun v(message: () -> String) = loggerImpl.v(message)
    fun d(message: () -> String) = loggerImpl.d(message)
    fun i(message: () -> String) = loggerImpl.i(message)
    fun w(message: () -> String) = loggerImpl.w(null, message)
    fun w(throwable: Throwable, message: () -> String) = loggerImpl.w(throwable, message)
    fun e(message: () -> String) = loggerImpl.e(null, message)
    fun e(throwable: Throwable, message: () -> String) = loggerImpl.e(throwable, message)

    // --- Java API (String-based) ---
    fun v(message: String) = loggerImpl.v { message }
    fun d(message: String) = loggerImpl.d { message }
    fun i(message: String) = loggerImpl.i { message }
    @JvmOverloads fun w(message: String, throwable: Throwable? = null) = loggerImpl.w(throwable) { message }
    @JvmOverloads fun e(message: String, throwable: Throwable? = null) = loggerImpl.e(throwable) { message }
}