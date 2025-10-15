package com.demo.core.view.banner.internal

import android.os.SystemClock
import androidx.viewpager2.widget.ViewPager2
import com.demo.core.view.banner.adapter.BannerAdapter
import com.demo.core.view.banner.adapter.BannerListAdapter

/**
 * (内部类) 实现了 BannerView 无限循环核心跳转逻辑的 OnPageChangeCallback。
 * 这个类对库的使用者是不可见的，它封装了所有复杂的页面切换处理。
 *
 * 功能包括:
 * 1.  在滑动到“假”页面时，执行无动画的“无缝跳转”。
 * 2.  内置防抖机制，防止用户一次快速猛滑导致两次跳转的“抖动”问题。
 * 3.  处理内部跳转 (`isInternalJump`) 和安全重置 (`isResetting`) 标志位，避免污染外部监听器。
 * 4.  将 ViewPager2 的虚拟位置转换为真实位置，并通知 BannerView 更新指示器。
 *
 * @param viewPager 需要应用此逻辑的 ViewPager2 实例。
 * @param onPageChanged 一个回调函数，当页面发生有效变化时，会携带真实位置被调用。
 */
internal class InfiniteLoopPageChangeCallback(
    private val viewPager: ViewPager2,
    private val onPageChanged: (realPosition: Int) -> Unit
) : ViewPager2.OnPageChangeCallback() {

    // 标志位，用于识别本次页面切换是否由我们的代码内部发起，以屏蔽不必要的回调。
    private var isInternalJump = false
    // 上次状态改变的时间戳，用于实现防抖。
    private var lastStateChangeTime: Long = 0L

    /**
     * 当滚动状态发生改变时被调用 (IDLE, DRAGGING, SETTLING)。
     * 我们的核心跳转逻辑在这里，当滚动停止 (IDLE) 时触发。
     */
    override fun onPageScrollStateChanged(state: Int) {
        // --- 防抖机制 ---
        // 记录当前时间戳。
        val now = SystemClock.elapsedRealtime()
        // 如果两次状态改变的间隔小于 80 毫秒，我们认为这是一次快速滑动中的“抖动”，直接忽略。
        if (now - lastStateChangeTime < 80) {
            return
        }
        // 更新上次状态改变的时间。
        lastStateChangeTime = now

        // --- 核心跳转逻辑 ---
        // 只有当滚动完全停止时，才检查是否需要跳转。
        if (state == ViewPager2.SCROLL_STATE_IDLE) {
            // 安全地获取适配器实例。
            val adapter = viewPager.adapter
            if (adapter !is BannerAdapter<*, *> && adapter !is BannerListAdapter<*, *>) return

            // 获取通用属性
            val isLoopingEnabled = if (adapter is BannerAdapter<*, *>) adapter.isLoopingEnabled() else (adapter as BannerListAdapter<*, *>).isLoopingEnabled()

            // 如果未启用循环，则不执行任何操作。
            if (!isLoopingEnabled) return

            val currentItem = viewPager.currentItem
            val itemCount = adapter.itemCount

            // 判断当前是否停在了“假”的页面上。
            val targetPosition = when (currentItem) {
                // 如果停在了虚拟头部 (位置0)，则目标是跳转到真实的尾部。
                0 -> itemCount - 2
                // 如果停在了虚拟尾部 (itemCount - 1)，则目标是跳转到真实的头部。
                itemCount - 1 -> 1
                // 否则，停在了真实页面，无需跳转。
                else -> -1
            }

            // 如果需要跳转...
            if (targetPosition != -1) {
                // 1. 设置标志位，让 onPageSelected 忽略这次由代码引起的页面变化。
                isInternalJump = true
                // 2. 执行无动画的、用户无感的页面跳转。
                viewPager.setCurrentItem(targetPosition, false)
            }
        }
    }

    /**
     * 当一个新的页面被选中时调用。
     * 我们在这里将虚拟位置转换为真实位置，并通知外部监听器（例如更新指示器）。
     */
    override fun onPageSelected(position: Int) {
        // 安全地获取适配器实例。
        val adapter = viewPager.adapter
        if (adapter !is BannerAdapter<*, *> && adapter !is BannerListAdapter<*, *>) return

        // --- 回调屏蔽逻辑 ---
        // 获取通用属性
        val isResetting = if (adapter is BannerAdapter<*, *>) adapter.isResetting else (adapter as BannerListAdapter<*, *>).isResetting

        // 如果这次 onPageSelected 是由我们的内部跳转或安全重置触发的，则直接忽略，不通知外部。
        if (isInternalJump || isResetting) {
            // 如果是安全重置触发的，消费掉这个标志位。
            if (isResetting) {
                if (adapter is BannerAdapter<*, *>) adapter.isResetting = false
                if (adapter is BannerListAdapter<*, *>) adapter.isResetting = false
            }
            // 消费掉内部跳转标志位。
            isInternalJump = false
            return
        }

        // --- 真实位置计算与通知 ---
        // 将 ViewPager2 报告的虚拟位置转换为真实数据的位置索引。
        val realPosition = if (adapter is BannerAdapter<*, *>) {
            adapter.getRealPosition(position)
        } else {
            (adapter as BannerListAdapter<*, *>).getRealPosition(position)
        }

        // 如果真实位置有效，则调用传入的回调函数。
        if (realPosition != -1) {
            onPageChanged.invoke(realPosition)
        }
    }
}