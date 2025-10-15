package com.demo.core.view.banner.internal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/** (内部类) 监听宿主生命周期，自动控制 Banner 的启动和停止。 */
internal class BannerLifecycleObserver(private val bannerView: BannerView) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        bannerView.startAutoLoop()
    }
    override fun onStop(owner: LifecycleOwner) {
        bannerView.stopAutoLoop()
    }
}