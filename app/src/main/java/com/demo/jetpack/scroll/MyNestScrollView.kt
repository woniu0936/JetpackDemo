package com.demo.jetpack.scroll

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat.NestedScrollType
import androidx.core.view.ViewCompat.ScrollAxis
import androidx.core.widget.NestedScrollView
import com.demo.core.logger.logD

/**
 * 原理参见：https://blog.csdn.net/lmj623565791/article/details/52204039
 * github地址：https://github.com/hongyangAndroid/Android-StickyNavLayout
 */
class MyNestScrollView : NestedScrollView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onStartNestedScroll(
        child: View, target: View, @ScrollAxis axes: Int,
        @NestedScrollType type: Int
    ): Boolean {
        val result = super.onStartNestedScroll(child, target, axes, type)
//        logD("IOIIOIO") { "onStartNestedScroll, result: $result, child: ${child.javaClass.simpleName}, target: ${target.javaClass.simpleName}, axes: $axes, type: $type" }
//        return if (canScrollVertically(-1) || canScrollVertically(1)) {
//            result
//        } else {
//            false
//        }
        return result
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int): Boolean {
        val result = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
//        logD("IOIIOIO") { "dispatchNestedPreScroll, result: $result, dy: ${dy}, consumed: ${consumed}, offsetInWindow: $offsetInWindow, type: $type" }

//        if (dy > 0) {
//            // 手指向上滑动
//            if (canScrollVertically(1)) {
//                consumed?.set(1, dy)
//            }
//        } else {
//            // 手指向下滑动
////            if (canScrollVertically(-1)) {
////                consumed?.set(1, dy)
////            }
//        }
        return result
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, @NestedScrollType type: Int, consumed: IntArray
    ) {
//        logD("IOIIOIO") { "onNestedScroll, target: ${target.javaClass.simpleName}" }
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
//        logD("IOIIOIO") {
//            "onNestedPreScroll, target: ${target.javaClass.simpleName}, dy: ${dy}, consumed: ${consumed}, type: $type， ${canScrollVertically(1)}---${
//                canScrollVertically(
//                    -1
//                )
//            }"
//        }
        val up = dy > 0 && canScrollVertically(1)
        if (up) {
            scrollBy(0, dy)
            consumed.set(1, dy)
        }
        logD("IOIIOIO") { "onNestedPreScroll, up: $up, dy: $dy, canScrollVertically(1): ${canScrollVertically(1)}" }
        super.onNestedPreScroll(target, dx, dy, consumed, type)
    }


}