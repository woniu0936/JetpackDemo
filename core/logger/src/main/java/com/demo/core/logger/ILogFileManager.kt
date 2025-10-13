package com.demo.core.logger

import android.content.Context

/**
 * 定义日志文件管理操作（如分享或刷新日志）契约的接口。
 *
 * 此接口抽象了日志文件的具体管理实现，使得日志系统可以灵活地替换不同的文件管理策略。
 *
 * @see AppLogger
 * @see LogFileManagerImpl
 */
internal interface ILogFileManager {
    /**
     * 将最近指定天数的日志文件打包并提供分享。
     *
     * 此方法通常会收集指定日期范围内的日志文件，将它们压缩成一个 ZIP 文件，
     * 并通过 Android 的分享机制（Intent）提供给用户进行分享。
     *
     * @param context 用于启动分享意图的上下文。
     * @param days 要包含的最近日志天数。
     *
     * @example
     * ```kotlin
     * // 示例：通过 AppLogger 分享最近两天的日志
     * AppLogger.shareRecentLogs(applicationContext, 2)
     * ```
     */
    fun shareRecentLogs(context: Context, days: Int)

    /**
     * 同步地将所有缓冲的日志写入磁盘。
     *
     * 此操作会强制日志系统将内存中所有未写入磁盘的日志数据立即刷新到对应的日志文件中。
     * 调用此方法会阻塞当前线程，直到所有日志写入完成。
     * 在性能敏感的场景下应谨慎使用，避免阻塞主线程。
     *
     * @example
     * ```kotlin
     * // 示例：在应用程序即将关闭时，确保所有日志都已写入磁盘
     * AppLogger.flushSync()
     * ```
     */
    fun flushSync()
}