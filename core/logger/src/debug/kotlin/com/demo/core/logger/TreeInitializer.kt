package com.demo.core.logger

import timber.log.Timber

/**
 * [调试专用] 提供日志树（Timber Trees）的实际初始化实现。
 *
 * 此函数负责根据 [LogConfig] 配置，向 [Timber] 框架种植（plant）不同的日志树。
 * 它确保在调试（debug）版本中，至少会种植一个 [Timber.DebugTree] 用于 Logcat 输出，
 * 并且如果配置允许，还会种植 [FileTree] 用于文件日志记录。
 *
 * @param config 日志配置对象，决定了哪些日志树应该被种植。
 *
 * @see AppLogger.init
 * @see Timber
 * @see FileTree
 */
internal fun initializeTrees(config: LogConfig) {
    // 如果已经种植了日志树，则直接返回，避免重复初始化
    if (Timber.treeCount > 0) return

    // 总是种植一个 DebugTree，确保日志输出到 Logcat
    Timber.plant(Timber.DebugTree())

    // 如果配置中启用了文件日志，则种植 FileTree
    if (config.enableFileLogging) {
        Timber.plant(FileTree(config))
    }

    /**
     * @example
     * ```kotlin
     * // 此函数通常在 AppLogger.init 内部被调用，不直接暴露给外部使用。
     * // 示例伪代码：
     * fun initAppLogger(context: Context, config: LogConfig) {
     *     // ... 其他初始化逻辑
     *     initializeTrees(config) // 在这里调用
     *     // ...
     * }
     * ```
     */
}
