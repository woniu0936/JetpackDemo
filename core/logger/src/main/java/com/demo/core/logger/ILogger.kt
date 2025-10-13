package com.demo.core.logger

import android.content.Context

interface ILogger {
    fun init(context: Context)
    fun d(tag: String, msg: () -> String)
    fun i(tag: String, msg: () -> String)
    fun w(tag: String, msg: () -> String, tr: Throwable? = null)
    fun e(tag: String, msg: () -> String, tr: Throwable? = null)
}