package com.demo.core.logger

import com.novel.library.Libs
import timber.log.Timber

internal object LoggerImpl : ILogger {

    init {
        if (Timber.treeCount == 0) {
            val logsDir = LogFileManager.logsDir(Libs.application())
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileTree(logsDir))
            CrashFileTree(logsDir).plant()
        }
    }

    override fun d(tag: String, msg: () -> String) { Timber.tag(tag).d(msg()) }
    override fun i(tag: String, msg: () -> String) { Timber.tag(tag).i(msg()) }
    override fun w(tag: String, msg: () -> String, tr: Throwable?) { Timber.tag(tag).w(tr, msg()) }
    override fun e(tag: String, msg: () -> String, tr: Throwable?) { Timber.tag(tag).e(tr, msg()) }
}