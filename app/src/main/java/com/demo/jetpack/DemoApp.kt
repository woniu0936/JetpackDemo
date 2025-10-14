package com.demo.jetpack

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.mvrx.Mavericks
import com.demo.core.crash.CrashManager
import com.demo.core.logger.AppLogger
import com.demo.core.logger.logger
import com.demo.jetpack.lifecycle.AppLifecycle
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DemoApp : Application() {

    private val log by lazy { logger() }

    override fun onCreate() {
        super.onCreate()
        //可以用来做前后台监听
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycle)
        Mavericks.initialize(this)
        AppLogger.init(this) {
            // 在这里可以配置 AppLogger，例如启用文件日志、设置日志保留天数等
            // enableFileLogging(true)
            // retentionDays(7)
        }
        CrashManager.init(this) {
            onCrash { _ ->
                // 在应用崩溃时，强制刷新所有缓冲的日志到磁盘
                AppLogger.flushSync()
            }
        }

        log.d { "DemoApp 初始化完成。" }
    }

}