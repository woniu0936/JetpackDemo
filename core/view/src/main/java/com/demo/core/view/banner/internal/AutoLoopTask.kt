package com.demo.core.view.banner.internal

import java.lang.ref.WeakReference

/** (内部类) 负责执行自动循环任务的 Runnable。 */
internal class AutoLoopTask(banner: BannerView) : Runnable {
    private val reference: WeakReference<BannerView> = WeakReference(banner)
    override fun run() {
        val banner = reference.get()
        if (banner != null && banner.isAutoLoopEnabled) {
            banner.scrollToNextPage()
            banner.postDelayed(this, banner.autoLoopIntervalMillis)
        }
    }
}