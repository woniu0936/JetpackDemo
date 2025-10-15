package com.demo.core.view.banner.transformer

import androidx.viewpager2.widget.ViewPager2

import android.view.View
import kotlin.math.abs

/**
 * 一个内置的画廊（廊模式）过渡动画。
 * 它会缩放相邻的页面，并为它们应用透明度，创造出景深效果，常用于“一屏多显”的场景。
 *
 * @param minScale 页面在最远处的最小缩放比例，取值范围 (0, 1)。
 * @param minAlpha 页面在最远处的最小透明度，取值范围 [0, 1]。
 *
 * @example
 * ```kotlin
 * // 使用默认参数创建一个画廊效果
 * bannerView.setPageTransformer(GalleryTransformer())
 *
 * // 自定义缩放和透明度，创造更强的景深感
 * bannerView.setPageTransformer(GalleryTransformer(minScale = 0.8f, minAlpha = 0.5f))
 * ```
 */
class GalleryTransformer(
    private val minScale: Float = 0.85f,
    private val minAlpha: Float = 0.65f
) : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // position:
        // [-1, 0] -> 页面从右侧进入屏幕
        // [0, 1]  -> 页面向左侧滑出屏幕
        // 0       -> 页面居中

        // 计算缩放比例，离中心越远，缩放比例越小，最小为 minScale
        val scaleFactor = minScale.coerceAtLeast(1 - abs(position))
        // 计算透明度，离中心越远，透明度越低，最小为 minAlpha
        val alphaFactor = minAlpha.coerceAtLeast(1 - abs(position))

        page.scaleX = scaleFactor
        page.scaleY = scaleFactor
        page.alpha = alphaFactor
    }
}