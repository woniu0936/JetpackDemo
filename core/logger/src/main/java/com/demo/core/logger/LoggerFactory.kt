package com.demo.core.logger

import java.util.concurrent.ConcurrentHashMap

/**
 * [Internal Factory] Creates and caches [Logger] instances.
 */
internal object LoggerFactory {
    private val loggers = ConcurrentHashMap<String, Logger>()

    fun getLogger(tag: String): Logger {
        return loggers.computeIfAbsent(tag) { newTag ->
            val loggerImpl = LoggerImpl(newTag)
            Logger(loggerImpl)
        }
    }

    fun getLogger(clazz: Class<*>): Logger {
        return getLogger(clazz.simpleName)
    }
}