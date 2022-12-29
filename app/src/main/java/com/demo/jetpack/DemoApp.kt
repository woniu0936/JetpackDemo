package com.demo.jetpack

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DemoApp : Application() {

    companion object {
        const val COMMON_TAG = "XXXXXXXXX"
    }

}