package com.demo.core.logger

object LoggerFactory {
    fun get(): ILogger = LoggerImpl

    @JvmStatic fun d(tag: String, msg: String) { get().d(tag) { msg } }
    @JvmStatic fun i(tag: String, msg: String) { get().i(tag) { msg } }
    @JvmStatic fun w(tag: String, msg: String, tr: Throwable? = null) { get().w(tag, { msg }, tr) }
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable? = null) { get().e(tag, { msg }, tr) }
}