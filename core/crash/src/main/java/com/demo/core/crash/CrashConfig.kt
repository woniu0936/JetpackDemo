package com.demo.core.crash

/**
 * Configuration for the crash handling system.
 * Use `CrashManager.init { ... }` in Kotlin or `new CrashConfig.Builder()` in Java.
 */
class CrashConfig private constructor(
    val onCrashCallback: ((throwable: Throwable) -> Unit)?
) {
    /**
     * A Builder for constructing [CrashConfig] instances, friendly to both Kotlin and Java.
     */
    class Builder {
        private var onCrashCallback: ((throwable: Throwable) -> Unit)? = null

        /**
         * Sets a callback to be executed when an uncaught exception occurs.
         * This is the primary mechanism for flushing logs or reporting errors to third-party services.
         */
        fun onCrash(callback: (throwable: Throwable) -> Unit) = apply { this.onCrashCallback = callback }

        /** Builds and returns the immutable [CrashConfig] instance. */
        fun build() = CrashConfig(onCrashCallback)
    }
}