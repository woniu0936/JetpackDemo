package com.demo.core.view


import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.demo.core.logger.logI
import kotlin.math.abs

private const val LOG_TAG = "InfiniteScrollExt"

// --- ViewPager2 Extensions ---

/**
 * 为 ViewPager2 设置一个无限循环适配器。
 *
 * @param adapter 实现了 [IInfiniteAdapter] 接口的适配器实例。
 * @example
 * val bannerAdapter = MyBannerAdapter()
 * viewPager.setupInfiniteAdapter(bannerAdapter)
 */
fun ViewPager2.setupInfiniteAdapter(adapter: RecyclerView.Adapter<*>) {
    require(adapter is IInfiniteAdapter<*>) { "Adapter must implement IInfiniteAdapter." }
    this.adapter = adapter
}

/**
 * 在首次数据加载后，将 ViewPager2 跳转到适合无限滚动的起始位置。
 * 此方法是幂等的，内部通过 View Tag 确保只执行一次。
 * @example
 * bannerAdapter.submitList(bannerList)
 * viewPager.jumpToInfiniteStart() // 在数据提交后调用
 */
fun ViewPager2.jumpToInfiniteStart() {
    val infiniteAdapter = this.adapter as? IInfiniteAdapter<*> ?: return
    val isSet = getTag(R.id.infinite_scroll_initial_position_set) as? Boolean ?: false
    if (!isSet && infiniteAdapter.getRealCount() > 1) {
        val middlePosition = Int.MAX_VALUE / 2
        val offset = middlePosition % infiniteAdapter.getRealCount()
        val initialPosition = middlePosition - offset
        setCurrentItem(initialPosition, false)
        setTag(R.id.infinite_scroll_initial_position_set, true)
        logI(LOG_TAG) { "ViewPager2: Jumped to infinite start position $initialPosition." }
    }
}

/**
 * 智能地将 ViewPager2 的当前项设置为指定的真实位置。
 * 此方法会自动计算最短滚动路径，提供最自然的滚动体验。
 *
 * @param realPosition 要跳转到的真实数据位置。
 * @param smoothScroll 是否平滑滚动。
 * @example
 * bannerAdapter.onItemClickListener = { _, realPosition ->
 *     viewPager.setCurrentInfiniteItem(realPosition, true)
 * }
 */
fun ViewPager2.setCurrentInfiniteItem(realPosition: Int, smoothScroll: Boolean = true) {
    val adapter = this.adapter as? IInfiniteAdapter<*> ?: return
    val realCount = adapter.getRealCount()
    if (realCount <= 1 || realPosition !in 0 until realCount) return

    val currentVirtualItem = this.currentItem
    val currentRealItem = currentVirtualItem % realCount
    var diff = realPosition - currentRealItem
    if (abs(diff) > realCount / 2) {
        diff += if (diff > 0) -realCount else realCount
    }
    val targetVirtualItem = currentVirtualItem + diff
    if (this.currentItem != targetVirtualItem) {
        this.setCurrentItem(targetVirtualItem, smoothScroll)
    }
}

/**
 * 为 ViewPager2 添加一个页面变化监听器，回调中包含自动转换后的真实位置。
 *
 * @param listener 一个接收真实位置的回调 Lambda。
 * @example
 * viewPager.onInfinitePageChange { realPosition ->
 *     // 更新你的页面指示器
 *     indicator.selection = realPosition
 * }
 */
fun ViewPager2.onInfinitePageChange(listener: (realPosition: Int) -> Unit) {
    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val adapter = this@onInfinitePageChange.adapter as? IInfiniteAdapter<*>
            if (adapter != null && adapter.getRealCount() > 0) {
                listener.invoke(position % adapter.getRealCount())
            }
        }
    })
}

// --- RecyclerView Extensions ---

/**
 * 为 RecyclerView 设置无限循环适配器和布局管理器。
 */
fun RecyclerView.setupInfiniteAdapter(
    layoutManager: RecyclerView.LayoutManager,
    adapter: RecyclerView.Adapter<*>
) {
    require(adapter is IInfiniteAdapter<*>) { "Adapter must implement IInfiniteAdapter." }
    this.layoutManager = layoutManager
    this.adapter = adapter
}

/**
 * 将 RecyclerView (通常是横向) 滚动到适合无限滚动的起始位置。
 */
fun RecyclerView.jumpToInfiniteStart() {
    val infiniteAdapter = this.adapter as? IInfiniteAdapter<*> ?: return
    val layoutManager = this.layoutManager as? LinearLayoutManager ?: return
    val isSet = getTag(R.id.infinite_scroll_initial_position_set) as? Boolean ?: false
    if (!isSet && infiniteAdapter.getRealCount() > 1) {
        val middlePosition = Int.MAX_VALUE / 2
        val offset = middlePosition % infiniteAdapter.getRealCount()
        val initialPosition = middlePosition - offset
        layoutManager.scrollToPositionWithOffset(initialPosition, 0)
        setTag(R.id.infinite_scroll_initial_position_set, true)
        logI(LOG_TAG) { "RecyclerView: Jumped to infinite start position $initialPosition." }
    }
}