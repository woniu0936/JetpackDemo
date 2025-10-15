package com.demo.core.view.infinite


import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.demo.core.logger.logD
import com.demo.core.logger.logI
import com.demo.core.logger.logW
import com.demo.core.view.R
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

// --- 标准API: 操作真实位置 (Real Position) ---

/**
 * **【标准API】**
 * 智能地将 ViewPager2 的当前项设置为指定的 **真实位置**。
 * 使用默认的滚动速度。
 *
 * @param realPosition 要跳转到的真实数据位置。
 * @param smoothScroll 是否平滑滚动。
 */
fun ViewPager2.setCurrentInfiniteItem(realPosition: Int, smoothScroll: Boolean = true) {
    val adapter = this.adapter as? IInfiniteAdapter<*> ?: return
    val realCount = adapter.getRealCount()
    if (realCount <= 1 || realPosition !in 0 until realCount) {
        logW(LOG_TAG) { "setCurrentInfiniteItem failed: invalid realPosition or not enough items." }
        return
    }

    val currentVirtualItem = this.currentItem
    val currentRealItem = currentVirtualItem % realCount
    var diff = realPosition - currentRealItem
    if (abs(diff) > realCount / 2) {
        diff += if (diff > 0) -realCount else realCount
    }
    val targetVirtualItem = currentVirtualItem + diff

    // 调用新增的高级API来执行滚动
    setCurrentInfiniteRawItem(targetVirtualItem, smoothScroll)
}

/**
 * **【标准API】**
 * 智能地将 ViewPager2 的当前项设置为指定的 **真实位置**，并控制平滑滚动的**速度**。
 *
 * @param realPosition 要跳转到的真实数据位置。
 * @param duration 定义滚动的速度基准，即“滚动1000个像素点所需的时间（毫秒）”。
 */
fun ViewPager2.setCurrentInfiniteItem(realPosition: Int, duration: Long) {
    if (duration <= 0) {
        setCurrentInfiniteItem(realPosition, false)
        return
    }

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

    // 调用新增的高级API来执行滚动
    setCurrentInfiniteRawItem(targetVirtualItem, duration)
}


// --- 高级API: 操作原始/虚拟位置 (Raw/Virtual Position) ---

/**
 * **【高级API】**
 * 将 ViewPager2 的当前项直接设置为指定的 **虚拟位置**。
 *
 * **警告**: 这是一个底层API。调用者需要自行保证 `virtualPosition` 的有效性。
 * 除非有特殊需求，否则应优先使用操作真实位置的 `setCurrentInfiniteItem`。
 *
 * @param virtualPosition 要跳转到的 **虚拟/原始** 位置 (`adapterPosition`)。
 * @param smoothScroll 是否平滑滚动。
 */
fun ViewPager2.setCurrentInfiniteRawItem(virtualPosition: Int, smoothScroll: Boolean = true) {
    logD(LOG_TAG) {
        "[Raw][Default Speed] Set to virtualPosition=$virtualPosition, smoothScroll=$smoothScroll"
    }
    if (currentItem != virtualPosition) {
        this.setCurrentItem(virtualPosition, smoothScroll)
    }
}

/**
 * **【高级API】**
 * 将 ViewPager2 的当前项直接设置为指定的 **虚拟位置**，并控制平滑滚动的**速度**。
 *
 * **警告**: 这是一个底层API。调用者需要自行保证 `virtualPosition` 的有效性。
 * 除非有特殊需求，否则应优先使用操作真实位置的 `setCurrentInfiniteItem`。
 *
 * @param virtualPosition 要跳转到的 **虚拟/原始** 位置 (`adapterPosition`)。
 * @param duration 定义滚动的速度基准，即“滚动1000个像素点所需的时间（毫秒）”。
 */
fun ViewPager2.setCurrentInfiniteRawItem(virtualPosition: Int, duration: Long) {
    logD(LOG_TAG) {
        "[Raw][Custom Speed] Set to virtualPosition=$virtualPosition, duration=$duration"
    }
    if (currentItem == virtualPosition) {
        logD(LOG_TAG) { "Already at the target raw position. No scroll needed." }
        return
    }
    if (duration <= 0) {
        setCurrentInfiniteRawItem(virtualPosition, false)
        return
    }

    val recyclerView = this.getChildAt(0) as? RecyclerView ?: return
    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return

    val smoothScroller = ConsistentVelocityScroller(this.context, duration)

    smoothScroller.targetPosition = virtualPosition
    layoutManager.startSmoothScroll(smoothScroller)
}

/**
 * 一个自定义的 `LinearSmoothScroller`，其核心目标是提供一个**恒定的滚动速度 (Velocity)**。
 *
 * 这个滚动器的速度由一个外部传入的基准值 `durationPer1000px` 来定义，确保了无论滚动距离长短，
 * 用户感知到的动画速度都是一致的，从而提供了平滑且符合物理直觉的滚动体验。
 *
 * @param context 上下文环境。
 * @param durationPer1000px 定义滚动速度的基准。其含义是**“滚动1000个像素点所需的时间（毫秒）”**。
 *                          这个值越大，滚动速度越慢。
 */
private class ConsistentVelocityScroller(
    context: Context,
    private val durationPer1000px: Long
) : LinearSmoothScroller(context) {

    /**
     * `RecyclerView` 源码中用于计算默认滚动速度的基准常量。
     * 其含义是：滚动一英寸（inch）的距离，需要花费 25 毫秒。
     */
    companion object {
        private const val MILLISECONDS_PER_INCH = 25f
    }

    /**
     * 计算滚动一个像素所需的时间（毫秒）。
     * 这是 `LinearSmoothScroller` 控制滚动速度的核心方法。返回值越小，速度越快。
     *
     * @param displayMetrics 包含了屏幕密度等信息的对象。
     * @return 滚动一个像素需要花费的毫秒数。
     */
    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
        // 1. 获取 RecyclerView 默认的滚动速度（单位：毫秒/像素）。
        //    这是最可靠的基准，因为它已经考虑了当前设备的屏幕密度 (densityDpi)。
        val defaultSpeedPerPixel = MILLISECONDS_PER_INCH / displayMetrics.densityDpi

        // 2. 我们设定一个易于理解的、与设备无关的基准时长，用于比例计算。
        //    根据经验，RecyclerView 默认速度在多数设备上滚动1000px约需100ms。
        val baseDurationPer1000px = 100f

        // 3. 计算用户期望的速度与基准速度的比例。
        //    例如：如果用户传入 200L (期望更慢)，ratio = 200 / 100 = 2.0。
        //    如果用户传入 50L (期望更快)，ratio = 50 / 100 = 0.5。
        val speedRatio = durationPer1000px / baseDurationPer1000px

        // 4. 将默认速度乘以这个比例，得到最终的速度。
        //    - 如果 ratio > 1 (期望更慢)，每像素花费的时间就越长。
        //    - 如果 ratio < 1 (期望更快)，每像素花费的时间就越短。
        val finalSpeedPerPixel = defaultSpeedPerPixel * speedRatio

        logD(LOG_TAG) {
            "Calculating scroll speed -> " +
                    "defaultSpeed(ms/px): $defaultSpeedPerPixel, " +
                    "speedRatio: $speedRatio, " +
                    "finalSpeed(ms/px): $finalSpeedPerPixel"
        }

        return finalSpeedPerPixel
    }

    /**
     * 定义当目标视图（Item）滚动到屏幕可见区域时，如何对齐。
     * `SNAP_TO_START` 表示将目标视图的左边缘（对于水平列表）或上边缘（对于垂直列表）
     * 与 `RecyclerView` 的左边缘或上边缘对齐。
     * 这对于 `ViewPager2` 这种需要页面完全对齐的场景至关重要。
     */
    override fun getHorizontalSnapPreference(): Int {
        return SNAP_TO_START
    }

    /**
     * 同上，为垂直方向的滚动定义对齐方式。
     */
    override fun getVerticalSnapPreference(): Int {
        return SNAP_TO_START
    }
}