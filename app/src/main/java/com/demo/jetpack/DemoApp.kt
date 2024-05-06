package com.demo.jetpack

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.mvrx.Mavericks
import com.demo.jetpack.core.extension.initMMKV
import com.demo.jetpack.lifecycle.AppLifecycle
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DemoApp : Application() {

    companion object {
        const val COMMON_TAG = "XXXXXXXXX"
    }

    override fun onCreate() {
        super.onCreate()
        //可以用来做前后台监听
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycle)
        Mavericks.initialize(this)
        initMMKV(this)
    }

}