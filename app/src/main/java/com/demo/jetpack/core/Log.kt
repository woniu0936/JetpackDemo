package com.demo.jetpack.core

import android.util.Log
import com.demo.jetpack.BuildConfig
import com.demo.jetpack.DemoApp

inline fun logD(tag: String = DemoApp.COMMON_TAG, block: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.d(tag, block.invoke())
    }
}

