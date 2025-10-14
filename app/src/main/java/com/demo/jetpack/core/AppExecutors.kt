package com.demo.jetpack.core

import android.os.Handler
import android.os.Looper
import com.demo.core.logger.logger
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * 单例线程池封装，用来做线程处理。
 * 提供用于不同任务类型的线程池，例如单线程、磁盘 I/O 和主线程。
 */
object AppExecutors {

    private val log by lazy { logger() } // Add logger instance

    private val NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()

    @JvmStatic
    val single: Executor = Executors.newSingleThreadExecutor().also { log.d { "Single thread executor initialized." } }

    @JvmStatic
    val diskIO: Executor = Executors.newFixedThreadPool((NUMBER_OF_CORES + 1).coerceIn(2, 5)).also { log.d { "Disk I/O thread pool initialized." } }

    @JvmStatic
    val main: MainExecutor = MainThreadExecutor().also { log.d { "Main thread executor initialized." } }

    private class MainThreadExecutor : MainExecutor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())
        override fun executeDelay(delayTime: Long, command: Runnable) {
            log.d { "Executing command on main thread with delay: $delayTime ms" }
            mainThreadHandler.postDelayed(command, delayTime)
        }

        override fun removeCallbacks(command: Runnable) {
            mainThreadHandler.removeCallbacks(command)
        }

        override fun removeAll(command: Runnable) {
            mainThreadHandler.removeCallbacksAndMessages(null)
        }

        override fun execute(command: Runnable) {
            log.d { "Executing command on main thread." }
            mainThreadHandler.post(command)
        }
    }

    interface MainExecutor : Executor {

        /**
         * 延迟执行
         */
        fun executeDelay(delayTime: Long, command: Runnable)

        fun removeCallbacks(command: Runnable)

        fun removeAll(command: Runnable)
    }
}