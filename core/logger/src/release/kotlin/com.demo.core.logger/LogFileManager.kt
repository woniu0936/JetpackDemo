package com.demo.core.logger

import android.content.Context

object LogFileManager {
    fun shareRecentLogs(context: Context, authority: String, days: Int = 3) { /* no-op */ }
    fun shareCrashReport(context: Context, authority: String, days: Int = 3) { /* no-op */ }
    fun flushSync() { /* no-op */ }
}