package com.demo.core.crash

/**
 * Internal interface for the crash handler implementation.
 * This allows for different behaviors in debug and release builds.
 */
internal interface ICrashHandler : Thread.UncaughtExceptionHandler