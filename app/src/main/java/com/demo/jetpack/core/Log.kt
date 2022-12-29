package com.demo.jetpack.core

import android.util.Log
import com.demo.jetpack.DemoApp

fun logD(tag: String = DemoApp.COMMON_TAG, block: () -> String) {
    Log.d(tag, block.invoke())
}