package com.demo.core.logger

import android.content.Context
import timber.log.Timber

/**
 * The DEBUG implementation of the initializer. It uses the [LogConfig]
 * to conditionally plant the necessary Timber trees.
 */
internal fun initializeImpl(context: Context, config: LogConfig) {
    if (Timber.treeCount > 0) return

    // 1. 总是安装控制台日志树
    Timber.plant(Timber.DebugTree())

    // 2. 根据配置，决定是否安装文件日志树
    if (config.enableFileLogging) {
        // 假设 FileTree 的构造函数现在接受 config
        Timber.plant(FileTree(config))
    }

    // 3. 根据配置，决定是否安装崩溃捕获树
    if (config.enableCrashReporting) {
        // 假设 CrashFileTree 的构造函数现在也接受 config
//         Timber.plant(CrashFileTree(context, config))
    }
}