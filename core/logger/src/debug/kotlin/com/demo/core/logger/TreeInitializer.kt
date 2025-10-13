package com.demo.core.logger

import timber.log.Timber

/**
 * [Debug] Provides the actual implementation for tree initialization.
 */
internal fun initializeTrees(config: LogConfig) {
    if (Timber.treeCount > 0) return

    Timber.plant(Timber.DebugTree())

    if (config.enableFileLogging) {
        Timber.plant(FileTree(config))
    }
}