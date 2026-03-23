package com.demo.core.common.shake

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class LifecycleShakeDetector(
    private val lifecycleOwner: LifecycleOwner,
    onShake: () -> Unit
) : ShakeDetector(
    // 自动从 LifecycleOwner 中提取 Context
    context = when (lifecycleOwner) {
        is Activity -> lifecycleOwner
        is Fragment -> lifecycleOwner.requireContext()
        else -> throw IllegalArgumentException("LifecycleOwner must be Activity or Fragment")
    },
    onShake = onShake
), DefaultLifecycleObserver {

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        start()
    }

    override fun onPause(owner: LifecycleOwner) {
        stop()
    }
}