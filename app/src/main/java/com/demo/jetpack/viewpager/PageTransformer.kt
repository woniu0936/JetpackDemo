package com.demo.jetpack.viewpager

import android.view.View
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.scopes.ActivityScoped
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