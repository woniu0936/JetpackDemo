package com.demo.core.view.banner.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

/**
 * 一个简单的缩放过渡动画。
 * 离开屏幕的页面会均匀缩小。
 *
 * @param minScale 最小缩放比例。
 *
 * @example
 * ```kotlin
 * bannerView.setPageTransformer(ScaleTransformer())
 * ```
 */
class ScaleTransformer(private val minScale: Float = 0.85f) : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // 根据页面位置计算一个线性的缩放比例
        val scale = if (position < 0) {
            // 页面向左滑，position 从 0 到 -1
            (1 - minScale) * position + 1
        } else {
            // 页面向右滑，position 从 0 到 1
            (minScale - 1) * position + 1
        }
        page.scaleX = scale
        page.scaleY = scale
    }
}