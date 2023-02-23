package com.demo.jetpack.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.demo.jetpack.core.logD

//监听app前后台
object AppLifecycle : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        //只会调用一次
        logD(TAG) { "onCreate: $owner" }
    }

    override fun onResume(owner: LifecycleOwner) {
        logD(TAG) { "onResume: $owner" }
    }

    override fun onPause(owner: LifecycleOwner) {
        logD(TAG) { "onPause: $owner" }
    }

    override fun onStart(owner: LifecycleOwner) {
        //进入前台
        logD(TAG) { "onStart: $owner" }
    }

    override fun onStop(owner: LifecycleOwner) {
        // 进入后台
        logD(TAG) { "onStop: $owner" }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // 不会调用
        logD(TAG) { "onDestroy: $owner" }
    }

    /**
     * 判断app是否处于前台
     */
    @JvmStatic
    public fun isAppForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private const val TAG = "AppLifecycle"

}