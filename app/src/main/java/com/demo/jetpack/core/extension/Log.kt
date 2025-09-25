package com.demo.jetpack.core.extension

import android.util.Log
import com.demo.jetpack.BuildConfig
import com.demo.jetpack.DemoApp

inline fun logD(tag: String = DemoApp.COMMON_TAG, block: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.d(tag, block.invoke())
    }
}

inline fun logW(tag: String = DemoApp.COMMON_TAG, block: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.w(tag, block.invoke())
    }
}

inline fun logW(tag: String = DemoApp.COMMON_TAG, throws: Throwable, block: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.w(tag, block.invoke(), throws)
    }
}

inline fun logE(tag: String = DemoApp.COMMON_TAG, block: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.e(tag, block.invoke())
    }
}

inline fun logE(tag: String = DemoApp.COMMON_TAG, throws: Throwable, block: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.e(tag, block.invoke(), throws)
    }
}

