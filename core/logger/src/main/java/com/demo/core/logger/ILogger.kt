package com.demo.core.logger

interface ILogger {
    fun d(tag: String, msg: () -> String)
    fun i(tag: String, msg: () -> String)
    fun w(tag: String, tr: Throwable? = null, msg: () -> String)
    fun e(tag: String, tr: Throwable? = null, msg: () -> String)
}