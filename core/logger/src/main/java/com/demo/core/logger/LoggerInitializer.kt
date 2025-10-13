package com.demo.core.logger

import android.content.Context

/**
 * [Internal Initializer] Delegates initialization to the build-variant-specific implementation.
 */
internal object LoggerInitializer {
    // 将 config 参数向下传递
    fun initialize(context: Context, config: LogConfig) {
        initializeImpl(context, config)
    }
}