package com.novel.library.log

internal object LoggerImpl : ILogger {
    override fun d(tag: String, msg: () -> String) { /* no-op */ }
    override fun i(tag: String, msg: () -> String) { /* no-op */ }
    override fun w(tag: String, msg: () -> String, tr: Throwable?) { /* no-op */ }
    override fun e(tag: String, msg: () -> String, tr: Throwable?) { /* no-op */ }
}