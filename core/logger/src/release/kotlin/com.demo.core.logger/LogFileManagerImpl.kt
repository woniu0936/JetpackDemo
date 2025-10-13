package com.demo.core.logger

import android.content.Context

/**
 * The RELEASE implementation of [ILogFileManager]. All methods are empty (no-op).
 */
internal class LogFileManagerImpl(
    private val config: LogConfig // 构造函数签名保持一致
) : ILogFileManager {
    override fun shareRecentLogs(context: Context, days: Int) { /* No-op */ }
    override fun flushSync() { /* No-op */ }
}