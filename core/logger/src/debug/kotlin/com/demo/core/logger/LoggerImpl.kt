package com.demo.core.logger

import timber.log.Timber

/**
 * The DEBUG implementation of [ILogger]. It uses Timber to dispatch logs.
 */
internal class LoggerImpl(private val tag: String) : ILogger {
    override fun v(message: () -> String) = Timber.tag(tag).v(message())
    override fun d(message: () -> String) = Timber.tag(tag).d(message())
    override fun i(message: () -> String) = Timber.tag(tag).i(message())

    override fun w(throwable: Throwable?, message: () -> String) = Timber.tag(tag).w(throwable, message())
    override fun e(throwable: Throwable?, message: () -> String) = Timber.tag(tag).e(throwable, message())
}