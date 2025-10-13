package com.demo.core.logger

val Any.logger: Logger
    get() = LoggerFactory.getLogger(this.javaClass.simpleName)

private val topLevelLogger = LoggerFactory.getLogger("TopLevel")

fun logd(tag: String? = null, msg: () -> String) {
    (tag?.let { LoggerFactory.getLogger(it) } ?: topLevelLogger).d(msg)
}

fun logi(tag: String? = null, msg: () -> String) {
    (tag?.let { LoggerFactory.getLogger(it) } ?: topLevelLogger).i(msg)
}

fun logw(tag: String? = null, msg: () -> String, tr: Throwable? = null) {
    (tag?.let { LoggerFactory.getLogger(it) } ?: topLevelLogger).w(tr, msg)
}

fun loge(tag: String? = null, msg: () -> String, tr: Throwable? = null) {
    (tag?.let { LoggerFactory.getLogger(it) } ?: topLevelLogger).e(tr, msg)
}
