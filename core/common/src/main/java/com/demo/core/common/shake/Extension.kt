package com.demo.core.common.shake

import androidx.lifecycle.LifecycleOwner

fun LifecycleOwner.registerShakeDetector(onShake: () -> Unit) {
    LifecycleShakeDetector(this, onShake)
}