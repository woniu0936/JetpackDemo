package com.demo.core.logger

interface ILogger {
    fun d(tag: String, msg: () -> String)
    fun i(tag: String, msg: () -> String)
    fun w(tag: String, msg: () -> String, tr: Throwable? = null)
    fun e(tag: String, msg: () -> String, tr: Throwable? = null)
}