package com.demo.core.crash

import android.content.Context
import kotlin.system.exitProcess

/**
 * DEBUG implementation of [ICrashHandler].
 * It saves the crash report, executes callbacks, and then launches a UI activity.
 */
internal class CrashHandlerImpl(
    private val context: Context,
    private val config: CrashConfig
) : ICrashHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            config.onCrashCallback?.invoke(throwable)
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}