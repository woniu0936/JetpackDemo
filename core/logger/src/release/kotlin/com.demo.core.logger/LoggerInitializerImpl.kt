package com.demo.core.logger

import android.content.Context

/**
 * The RELEASE implementation of the initializer. It does nothing,
 * ignoring the config object.
 */
// 签名必须匹配，但方法体为空
internal fun initializeImpl(context: Context, config: LogConfig) {
    // No-op in release builds. The config parameter is unused and will be optimized away.
}