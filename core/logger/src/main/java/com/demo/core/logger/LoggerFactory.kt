package com.demo.core.logger

import java.util.concurrent.ConcurrentHashMap

object LoggerFactory {

    private val loggers = ConcurrentHashMap<String, Logger>()

    internal fun get(): ILogger = LoggerImpl

    fun getLogger(tag: String): Logger {
        return loggers.getOrPut(tag) { Logger(tag) }
    }

    fun getLogger(clazz: Class<*>): Logger {
        return getLogger(clazz.simpleName)
    }

    @Deprecated("Use getLogger(tag).d { msg } instead", ReplaceWith("getLogger(tag).d { msg }"))
    @JvmStatic
    fun d(tag: String, msg: String) {
        get().d(tag) { msg }
    }

    @Deprecated("Use getLogger(tag).i { msg } instead", ReplaceWith("getLogger(tag).i { msg }"))
    @JvmStatic
    fun i(tag: String, msg: String) {
        get().i(tag) { msg }
    }

    @Deprecated("Use getLogger(tag).w { msg } instead", ReplaceWith("getLogger(tag).w({ msg }, tr)"))
    @JvmStatic
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        get().w(tag, tr, { msg })
    }

    @Deprecated("Use getLogger(tag).e { msg } instead", ReplaceWith("getLogger(tag).e({ msg }, tr)"))
    @JvmStatic
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        get().e(tag, tr, { msg })
    }
}