package com.demo.core.crash

import android.content.Context

/**
 * [Public Facade] The single, public entry point for the crash handling module.
 */
object CrashManager {

    @Volatile
    private var isInitialized = false
    private val lock = Any()

    /**
     * Initializes the crash handling system. Must be called once in `Application.onCreate()`.
     */
    fun init(context: Context, block: CrashConfig.Builder.() -> Unit = {}) {
        val config = CrashConfig.Builder().apply(block).build()
        performInitialization(context, config)
    }

    /**
     * [For Java] Initializes with a [CrashConfig] object.
     */
    @JvmStatic
    fun init(context: Context, config: CrashConfig) {
        performInitialization(context, config)
    }

    /**
     * [For Simplicity] Initializes with default configuration.
     */
    @JvmStatic
    fun init(context: Context) {
        performInitialization(context, CrashConfig.Builder().build())
    }

    private fun performInitialization(context: Context, config: CrashConfig) {
        synchronized(lock) {
            if (isInitialized) return
            // The magic happens here: The compiler links to the correct
            // CrashHandlerImpl class based on the current build variant.
            val handler = CrashHandlerImpl(context.applicationContext, config)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            isInitialized = true
        }
    }

    /**
     * [Debug Only] This method is defined as an extension function in the `debug` source set.
     * It will not exist in release builds, causing a compile error if called from release code.
     *
     * In Kotlin: `CrashManager.shareCrashReport(this)`
     * In Java: `CrashManagerKt.shareCrashReport(CrashManager.INSTANCE, this)`
     */
    // The actual function is now defined in the debug source set.
}