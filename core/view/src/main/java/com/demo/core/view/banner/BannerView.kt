package com.demo.core.view.banner

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.demo.core.view.R
import com.demo.core.view.banner.adapter.BannerAdapter
import com.demo.core.view.banner.adapter.BannerListAdapter
import com.demo.core.view.banner.indicator.Indicator
import com.demo.core.view.banner.internal.AutoLoopTask
import com.demo.core.view.banner.internal.BannerLifecycleObserver
import com.demo.core.view.banner.internal.internalSetup
import com.demo.core.view.banner.internal.internalSubmitData
import com.demo.core.view.banner.internal.internalTeardown
import com.demo.core.view.banner.transformer.GalleryTransformer
import com.demo.core.view.banner.transformer.ScaleTransformer
import java.lang.ref.WeakReference

/**
 * 一个功能强大、可自定义且生命周期安全的商业级 Banner 视图。
 * 它支持无限循环、自动滚动、可插拔的指示器和页面过渡动画，并提供了两种适配器模式以应对不同场景。
 *
 * @constructor 创建一个 BannerView。
 *
 * @example
 * ```xml
 * <!-- 1. 在布局文件中添加 BannerView，并可通过 app 属性进行基础配置 -->
 * <com.yourcompany.banner.BannerView
 *     android:id="@+id/bannerView"
 *     android:layout_width="match_parent"
 *     android:layout_height="200dp"
 *     app:banner_loopTime="3000"
 *     app:banner_isAutoLoop="true"
 *     app:banner_transformer="gallery" />
 * ```
 *
 * ```kotlin
 * // 2. 在你的 Activity/Fragment 中
 * val bannerView = findViewById<BannerView>(R.id.bannerView)
 * val myAdapter = ImageBannerAdapter() // 你的适配器，继承自 BannerAdapter 或 BannerListAdapter
 *
 * // 3. 使用链式调用进行配置
 * bannerView.setAdapter(myAdapter)
 *     .setIndicator(CircleIndicator(this), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
 *     .setPageTransformer(GalleryTransformer(minScale = 0.9f)) // 设置或覆盖动画
 *     .attachToLifecycle(viewLifecycleOwner) // 推荐！自动管理生命周期
 *
 * // 4. 提交数据
 * myAdapter.submitList(listOf("url1", "url2", "url3"))
 *
 * // 5. 设置点击事件
 * bannerView.onItemClickListener = { data, position ->
 *     // 'data' 是被点击项的数据对象，'position' 是其在真实列表中的位置
 *     val bannerItem = data as BannerItem
 *     // 执行跳转或埋点等操作
 * }
 * ```
 */
class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Banner 内部用于滚动的 ViewPager2 实例。
    private val viewPager: ViewPager2 = ViewPager2(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    // --- 内部状态变量 ---
    private var adapterRef: WeakReference<RecyclerView.Adapter<*>>? = null
    private var indicator: Indicator? = null
    private var autoLoopTask = AutoLoopTask(this)
    private var bannerLifecycleObserver = BannerLifecycleObserver(this)
    private var lifecycleOwner: LifecycleOwner? = null

    // --- 公开的可配置属性 ---

    /**
     * 自动轮播时，每个页面静止停留的时长，单位为毫秒。
     * 只有在 [isAutoLoopEnabled] 为 `true` 时生效。
     * @default 3000L (3 秒)
     */
    var autoLoopIntervalMillis: Long = 3000L

    /**
     * 是否启用自动循环播放。
     * @default true
     */
    var isAutoLoopEnabled: Boolean = true

    /**
     * 页面之间切换滚动的动画时长，单位为毫秒。
     * **注意**: `ViewPager2` 并未提供直接修改此值的公开 API。
     * 此属性为未来通过反射等技术实现此功能保留，当前版本中修改它可能不会生效。
     * @default 800
     */
    var scrollAnimationDurationMillis: Int = 800

    /**
     * `ViewPager2` 在可见页面两侧预加载的页面数量。
     * 增加此值可以使快速滑动更流畅，但会增加内存消耗。
     * @default 1
     */
    var offscreenPageLimit: Int = 1

    /**
     * 设置 item 点击事件的监听器。
     * @param listener 一个 lambda 表达式，当 item 被点击时会携带数据和真实位置被调用。
     */
    var onItemClickListener: ((data: Any, realPosition: Int) -> Unit)? = null

    init {
        addView(viewPager)
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.BannerView)
            autoLoopIntervalMillis = a.getInt(R.styleable.BannerView_banner_loopTime, 3000).toLong()
            isAutoLoopEnabled = a.getBoolean(R.styleable.BannerView_banner_isAutoLoop, true)
            offscreenPageLimit = a.getInt(R.styleable.BannerView_banner_offscreenPageLimit, 1)
            if (a.hasValue(R.styleable.BannerView_banner_transformer)) {
                when (a.getInt(R.styleable.BannerView_banner_transformer, 0)) {
                    0 -> setPageTransformer(GalleryTransformer())
                    1 -> setPageTransformer(ScaleTransformer())
                }
            }
            a.recycle()
        }
    }

    /**
     * 设置 Banner 的适配器。这是让 Banner 工作的首要步骤。
     * 数据应通过适配器自身的 `submitList` 方法提交。
     *
     * @param adapter 你的适配器实例，可以是 [com.demo.core.view.banner.adapter.BannerAdapter] 或 [com.demo.core.view.banner.adapter.BannerListAdapter] 的子类。
     * @return [BannerView] 实例，以支持链式调用。
     */
    fun setAdapter(adapter: RecyclerView.Adapter<*>): BannerView {
        if (adapter !is BannerAdapter<*, *> && adapter !is BannerListAdapter<*, *>) {
            throw IllegalArgumentException("Adapter must be a subclass of BannerAdapter or BannerListAdapter.")
        }
        adapterRef = WeakReference(adapter)
        viewPager.internalSetup(adapter, offscreenPageLimit)

        // 通过 BannerView 统一暴露 item 点击事件
        val itemClickListener: (Any, Int) -> Unit = { data, realPosition ->
            this.onItemClickListener?.invoke(data, realPosition)
        }
        when (adapter) {
            is BannerAdapter<*, *> -> adapter.onItemClickListener = itemClickListener
            is BannerListAdapter<*, *> -> adapter.onItemClickListener = itemClickListener
        }

        // 如果适配器在设置时已经有数据，则触发一次提交
        val data = when (adapter) {
            is BannerAdapter<*, *> -> adapter.getRealData()
            is BannerListAdapter<*, *> -> adapter.getRealData()
            else -> emptyList()
        }
        if (data.isNotEmpty()) {
            viewPager.internalSubmitData(data)
        }
        return this
    }

    /**
     * 为 BannerView 提交或更新数据列表。
     * 这是从外部更新数据的推荐入口。
     *
     * @param data 新的数据列表。
     */
    fun submitData(data: List<*>) {
        // 调用内部的扩展函数来执行核心逻辑
        viewPager.internalSubmitData(data)
    }

    /**
     * 设置 Banner 的页面过渡动画。
     *
     * @param transformer 一个实现了 [BannerPageTransformer] 接口的对象。
     * @return [BannerView] 实例，以支持链式调用。
     * @example
     * ```kotlin
     * // 使用内置的画廊效果
     * bannerView.setPageTransformer(GalleryTransformer())
     *
     * // 使用内置的缩放效果
     * bannerView.setPageTransformer(ScaleTransformer())
     *
     * // 使用自定义效果
     * class MyFadeTransformer : BannerPageTransformer {
     *     override fun transformPage(page: View, position: Float) {
     *         page.alpha = 1 - abs(position)
     *     }
     * }
     * bannerView.setPageTransformer(MyFadeTransformer())
     * ```
     */
    fun setPageTransformer(transformer: ViewPager2.PageTransformer): BannerView {
        viewPager.setPageTransformer(transformer)
        return this
    }

    /**
     * 设置 Banner 的指示器。
     *
     * @param indicator 一个实现了 [Indicator] 接口的对象。
     * @param gravity 指示器在 Banner 中的位置 (例如, `Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL`)。
     * @return [BannerView] 实例，以支持链式调用。
     */
    fun setIndicator(indicator: Indicator, gravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL): BannerView {
        // 先移除可能已存在的旧指示器
        this.indicator?.getIndicatorView()?.let { removeView(it) }
        this.indicator = indicator
        val indicatorView = indicator.getIndicatorView()
        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            this.gravity = gravity
            // 设置默认的外边距
            setMargins(16, 16, 16, 16)
        }
        addView(indicatorView, params)
        return this
    }

    /**
     * 将 Banner 的自动循环生命周期附加到一个 [LifecycleOwner] (例如, Activity 或 Fragment)。
     * 这是管理 Banner 生命周期的**强烈推荐**方式，可以避免内存泄漏和不必要的后台操作。
     *
     * @param owner [LifecycleOwner]，通常是 `this` (在 Activity 中) 或 `viewLifecycleOwner` (在 Fragment 中)。
     * @return [BannerView] 实例，以支持链式调用。
     */
    fun attachToLifecycle(owner: LifecycleOwner): BannerView {
        this.lifecycleOwner?.lifecycle?.removeObserver(bannerLifecycleObserver)
        this.lifecycleOwner = owner
        owner.lifecycle.addObserver(bannerLifecycleObserver)
        return this
    }

    /**
     * 手动开始自动循环播放。
     * 如果已通过 [attachToLifecycle] 绑定生命周期，则通常无需手动调用此方法。
     */
    fun startAutoLoop() {
        // [修复] 增加对 autoLoopIntervalMillis 的防御性判断，防止因配置错误导致 CPU 飙高。
        if (!isAutoLoopEnabled || autoLoopIntervalMillis <= 0) return
        stopAutoLoop()
        postDelayed(autoLoopTask, autoLoopIntervalMillis)
    }

    /**
     * 手动停止自动循环播放。
     * 如果已通过 [attachToLifecycle] 绑定生命周期，则通常无需手动调用此方法。
     */
    fun stopAutoLoop() {
        removeCallbacks(autoLoopTask)
    }

    // (内部方法) 安全地滚动到下一页。
    internal fun scrollToNextPage() {
        val adapter = viewPager.adapter ?: return
        if (adapter.itemCount == 0) return
        // [修复] 使用取模运算安全地滚动到下一页，避免越界。
        val nextPosition = (viewPager.currentItem + 1) % adapter.itemCount
        viewPager.currentItem = nextPosition
    }

    // (内部方法) 当页面变化时，通知指示器更新。
    internal fun onPageChanged(realPosition: Int) {
        val adapter = adapterRef?.get() ?: return
        val realCount = when (adapter) {
            is BannerAdapter<*, *> -> adapter.getRealCount()
            is BannerListAdapter<*, *> -> adapter.getRealCount()
            else -> 0
        }
        indicator?.onPageChanged(realCount, realPosition)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 如果没有绑定生命周期，则使用传统的 View 生命周期管理方式
        if (lifecycleOwner == null) startAutoLoop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // [修复] 彻底移除延时任务，确保后台无残留，避免违反应用市场“静音”政策。
        removeCallbacks(autoLoopTask)
        viewPager.internalTeardown()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 当用户触摸 Banner 时，暂停自动播放
        if (isAutoLoopEnabled) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> stopAutoLoop()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> startAutoLoop()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

}
