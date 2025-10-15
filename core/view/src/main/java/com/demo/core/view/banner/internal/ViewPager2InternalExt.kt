@file:JvmName("BannerViewExtensions")

package com.demo.core.view.banner.internal


import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.demo.core.logger.logD
import com.demo.core.logger.logE
import com.demo.core.logger.logW
import com.demo.core.view.R
import com.demo.core.view.banner.BannerView
import com.demo.core.view.banner.adapter.BannerAdapter
import com.demo.core.view.banner.adapter.BannerListAdapter

// 日志标签
private const val EXTENSIONS_TAG = "BannerInternalExt"
// RecyclerView 视图缓存的默认大小，用于优化画廊模式
private const val DEFAULT_ITEM_VIEW_CACHE_SIZE = 10
// LinearLayoutManager 预取 item 的默认数量，用于优化画廊模式
private const val DEFAULT_INITIAL_PREFETCH_COUNT = 3

/**
 * (内部使用) 为 ViewPager2 执行深度初始化和优化。
 * 这个扩展函数封装了所有对 ViewPager2 内部 RecyclerView 的高级设置。
 *
 * @param adapter BannerView 设置的适配器实例。
 * @param offscreenPageLimit 用户指定的离屏页面限制。
 */
internal fun ViewPager2.internalSetup(
    adapter: RecyclerView.Adapter<*>,
    offscreenPageLimit: Int
) {
    logD(EXTENSIONS_TAG) { "internalSetup called. Setting adapter and performing deep optimizations." }
    this.adapter = adapter
    this.offscreenPageLimit = offscreenPageLimit

    // 尝试访问并优化内部的 RecyclerView
    try {
        val recyclerView = this.getChildAt(0) as RecyclerView

        // 1. 增加视图缓存大小，以在内存中保留更多已创建的 ViewHolder。
        // 这是修复画廊模式下快速滑动出现白块问题的关键之一。
        recyclerView.setItemViewCacheSize(DEFAULT_ITEM_VIEW_CACHE_SIZE)
        logD(EXTENSIONS_TAG) { "Successfully set RecyclerView's itemViewCacheSize to $DEFAULT_ITEM_VIEW_CACHE_SIZE" }

        // 2. 增加初始预取 Item 的数量。
        // 这会强制 LayoutManager 提前创建并准备好即将进入屏幕的 Item。
        // 这是修复画廊模式下白块问题的另一个关键。
        (recyclerView.layoutManager as? LinearLayoutManager)?.let {
            it.initialPrefetchItemCount = DEFAULT_INITIAL_PREFETCH_COUNT
            logD(EXTENSIONS_TAG) { "Successfully set LinearLayoutManager's initialPrefetchItemCount to $DEFAULT_INITIAL_PREFETCH_COUNT" }
        }

    } catch (e: Exception) {
        logE(EXTENSIONS_TAG, e) { "Failed to perform deep optimization on inner RecyclerView." }
    }
}

/**
 * (内部使用) 向 ViewPager2 提交数据，并根据数据量动态管理无限循环机制。
 * 这是整个 BannerView 响应数据变化的核心逻辑。
 *
 * @param data 新的数据列表。
 */
@Suppress("UNCHECKED_CAST")
internal fun ViewPager2.internalSubmitData(data: List<*>) {
    logD(EXTENSIONS_TAG) { "internalSubmitData called with new data size: ${data.size}" }
    // 确认适配器类型
    val adapter = this.adapter
    if (adapter !is BannerAdapter<*, *> && adapter !is BannerListAdapter<*, *>) {
        throw IllegalStateException("The adapter is not a supported BannerAdapter or BannerListAdapter.")
    }

    // 检查当前是否处于循环状态，以及根据新数据判断是否应该进入循环状态
    val isCurrentlyLooping = getTag(R.id.banner_internal_callback_tag) != null
    val shouldLoopNow = data.size > 1

    logD(EXTENSIONS_TAG) { "State check: isCurrentlyLooping=$isCurrentlyLooping, shouldLoopNow=$shouldLoopNow" }

    when {
        // 场景 1: 之前未循环，但现在数据量足够，需要开启循环
        !isCurrentlyLooping && shouldLoopNow -> {
            logD(EXTENSIONS_TAG) { "ENABLING LOOP: Registering callback and setting initial item." }
            // 创建并注册核心的页面变化回调
            val callback = InfiniteLoopPageChangeCallback(this) { realPosition ->
                // 当页面变化时，通过 parent 向上通知 BannerView，以便更新指示器
                (this.parent as? BannerView)?.onPageChanged(realPosition)
            }
            setTag(R.id.banner_internal_callback_tag, callback)
            registerOnPageChangeCallback(callback)

            // 先提交数据，确保 itemCount 正确
            (adapter as BannerAdapter<Any, *>).submitList(data)

            // 使用 post 确保 ViewPager2 已经完成布局，然后再设置初始位置
            post {
                if (isAttachedToWindow) {
                    setCurrentItem(1, false)
                }
            }
            return // 提前返回，避免重复提交数据
        }
        // 场景 2: 之前在循环，但现在数据量不足，需要关闭循环
        isCurrentlyLooping && !shouldLoopNow -> {
            logD(EXTENSIONS_TAG) { "DISABLING LOOP: Unregistering callback." }
            internalTeardown() // teardown 会清理 callback 和 tag
        }
    }

    // 场景 3: 循环状态不变，或刚被关闭循环
    // 检查是否需要安全重置 (例如，数据从 N->1 时)
    if (!shouldLoopNow && this.currentItem != 0) {
        logW(EXTENSIONS_TAG) { "SAFE RESET TRIGGERED! Data changed to non-loopable. Resetting position to 0." }
        if (adapter is BannerAdapter<*, *>) adapter.isResetting = true
        if (adapter is BannerListAdapter<*, *>) adapter.isResetting = true
        this.setCurrentItem(0, false)
    }

    // 提交数据
    when(adapter) {
        is BannerAdapter<*, *> -> (adapter as BannerAdapter<Any, *>).submitList(data)
        is BannerListAdapter<*, *> -> (adapter as BannerListAdapter<Any, *>).submitList(data)
    }
}

/**
 * (内部使用) 彻底清理与无限循环相关的资源。
 * 在 View 被销毁或循环被禁用时调用。
 */
internal fun ViewPager2.internalTeardown() {
    logD(EXTENSIONS_TAG) { "internalTeardown called for ViewPager2 with id: $id" }
    // 反注册回调，防止内存泄漏
    (getTag(R.id.banner_internal_callback_tag) as? InfiniteLoopPageChangeCallback)?.let {
        unregisterOnPageChangeCallback(it)
        logD(EXTENSIONS_TAG) { "Callback unregistered." }
    }
    // 清理 tag，断开引用
    setTag(R.id.banner_internal_callback_tag, null)
    // 清理 RecyclerView 的缓存池，防止后台返回时因缓存的 ViewHolder 引用失效而出现白屏
    (getChildAt(0) as? RecyclerView)?.recycledViewPool?.clear()
}