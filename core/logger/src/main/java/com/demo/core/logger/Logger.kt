package com.demo.core.logger

class Logger internal constructor(private val tag: String) {

    fun d(msg: () -> String) {
        LoggerFactory.get().d(tag, msg)
    }

    fun i(msg: () -> String) {
        LoggerFactory.get().i(tag, msg)
    }

    fun w(tr: Throwable? = null, msg: () -> String) {
        LoggerFactory.get().w(tag, tr, msg)
    }

    fun e(tr: Throwable? = null, msg: () -> String) {
        LoggerFactory.get().e(tag, tr, msg)
    }

    // Java-friendly overloads
    @JvmOverloads
    fun d(msg: String) { d { msg } }
    @JvmOverloads
    fun i(msg: String) { i { msg } }
    @JvmOverloads
    fun w(msg: String, tr: Throwable? = null) { w(tr) { msg } }
    @JvmOverloads
    fun e(msg: String, tr: Throwable? = null) { e(tr) { msg } }
}