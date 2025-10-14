package com.demo.jetpack.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.demo.core.logger.logD
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class ActivityLifeComponent @Inject constructor() : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ActivityLifeComponent"
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        logD(TAG) {"------------------------------onCreate-----------------------------"}
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        logD(TAG) {"------------------------------onStart-----------------------------"}
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        logD(TAG) {"------------------------------onResume-----------------------------"}
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        logD(TAG) {"------------------------------onPause-----------------------------"}
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        logD(TAG) {"------------------------------onStop-----------------------------"}
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        logD(TAG) {"------------------------------onDestroy-----------------------------"}
    }

}