package com.demo.jetpack.lifecycle

import android.animation.ValueAnimator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner


fun ValueAnimator.bindLifecycle(owner: LifecycleOwner) {
    owner.lifecycle.addObserver(AnimatorLCO(this))
}

class AnimatorLCO(val va: ValueAnimator) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        va.resume()
    }

    override fun onPause(owner: LifecycleOwner) {
        va.pause()
        super.onPause(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        va.cancel()
        super.onDestroy(owner)
    }

}