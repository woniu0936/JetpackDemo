package com.demo.jetpack.viewpager

import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import javax.inject.Inject
import kotlin.math.abs

/**
 * 仿画廊效果,如下图的效果，横向滑动，就会有放大缩小的效果
 * ------------------------------------------------------------------------------------
 *                  ┌───────────────┐
 *  ┌───────────┐   │               │   ┌───────────┐
 *  │           │   │               │   │           │
 *  │           │   │               │   │           │
 *  │           │   │               │   │           │
 *  │           │   │               │   │           │
 *  │           │   │               │   │           │
 *  └───────────┘   │               │   └───────────┘
 *                  └───────────────┘
 * ------------------------------------------------------------------------------------
 */
class GalleryTransformer @Inject constructor() : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val absPos = abs(position)
        page.apply {
            if (position < 1F) {
                val scaleFactor = MIN_SCALE + (1 - absPos) * (MAX_SCALE - MIN_SCALE);
                scaleX = scaleFactor
                scaleY = scaleFactor
            } else {
                scaleX = MIN_SCALE
                scaleY = MIN_SCALE
            }
        }

    }

    companion object {
        private val MAX_SCALE = 1.0f;//0缩放
        private val MIN_SCALE = 0.80f;//0.85缩放
    }

}

class SliderTransformer @Inject constructor(private val offscreenPageLimit: Int) : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        page.apply {

            ViewCompat.setElevation(page, -abs(position))

            val scaleFactor = -SCALE_FACTOR * position + DEFAULT_SCALE
            val alphaFactor = -ALPHA_FACTOR * position + DEFAULT_ALPHA

            when {
                position <= 0f -> {
                    translationX = DEFAULT_TRANSLATION_X
                    scaleX = DEFAULT_SCALE
                    scaleY = DEFAULT_SCALE
                    alpha = DEFAULT_ALPHA + position
                }

                position <= offscreenPageLimit - 1 -> {
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    translationX = -(width / DEFAULT_TRANSLATION_FACTOR) * position
                    alpha = alphaFactor
                }

                else -> {
                    translationX = DEFAULT_TRANSLATION_X
                    scaleX = DEFAULT_SCALE
                    scaleY = DEFAULT_SCALE
                    alpha = DEFAULT_ALPHA
                }
            }
        }

    }

    companion object {

        private const val DEFAULT_TRANSLATION_X = .0f
        private const val DEFAULT_TRANSLATION_FACTOR = 1.15f

        private const val SCALE_FACTOR = .14f
        private const val DEFAULT_SCALE = 1f

        private const val ALPHA_FACTOR = .3f
        private const val DEFAULT_ALPHA = 1f
    }

}

class CarouselPageTransformer(private val itemSpacingPx: Int) : ViewPager2.PageTransformer {

    companion object {
        // 定义所有非中心页面的统一缩放比例
        private const val UNSELECTED_SCALE = 0.85f
    }

    override fun transformPage(page: View, position: Float) {
        page.apply {
            // 1. 设置缩放基准点在底部，以实现底部对齐 (保持不变)
            pivotY = height.toFloat()
            pivotX = width / 2f

            // 2. 【关键修正】修正缩放动画逻辑
            //    我们首先需要把 position 的绝对值“限制”在 [0, 1] 的范围内。
            //    这样，当页面位置 > 1 或 < -1 时，它在计算缩放时都会被当作 1 来处理。
            val clampedPositionAbs = abs(position).coerceIn(0f, 1f)

            //    使用这个被“限制”过的值来计算缩放，确保了：
            //    - 在 [-1, 1] 区间内，缩放是平滑过渡的。
            //    - 在 [-1, 1] 区间外，缩放值被固定在 UNSELECTED_SCALE。
            val scale = UNSELECTED_SCALE + (1 - clampedPositionAbs) * (1 - UNSELECTED_SCALE)
            scaleX = scale
            scaleY = scale

            // 3. 位移计算逻辑保持不变，因为它需要使用原始的、未被限制的 position
            //    来确保页面能被正确地移动到更远的位置。
            val translationX = position * -(page.width * (1 - scale) / 2 - itemSpacingPx)
            setTranslationX(translationX)

            // (可选优化) 确保中心页面在最上层
            translationZ = -abs(position)
        }
    }
}