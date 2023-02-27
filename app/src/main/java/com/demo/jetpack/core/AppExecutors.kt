package com.demo.jetpack.core

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * 单例线程池封装，用来做线程处理
 */
object AppExecutors {

    @JvmStatic
    val single: Executor = Executors.newSingleThreadExecutor()

    @JvmStatic
    val diskIO: Executor = Executors.newFixedThreadPool(3)

    @JvmStatic
    val main: MainExecutor = MainThreadExecutor()

    private class MainThreadExecutor : MainExecutor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())
        override fun executeDelay(delayTime: Long, command: Runnable) {
            mainThreadHandler.postDelayed(command, delayTime)
        }

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }

    interface MainExecutor : Executor {

        /**
         * 延迟执行
         */
        fun executeDelay(delayTime: Long, command: Runnable)
    }
}