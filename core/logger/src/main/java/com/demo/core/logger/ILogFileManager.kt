package com.demo.core.logger

import android.content.Context

/**
 * Interface defining the contract for log file management operations,
 * such as sharing or flushing logs.
 */
internal interface ILogFileManager {
    fun shareRecentLogs(context: Context, days: Int)
    fun shareCrashReport(context: Context, days: Int)
    fun flushSync()
}