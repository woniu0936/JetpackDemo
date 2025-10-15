package com.demo.core.view.banner.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.demo.core.view.base.BindingViewHolder

/**
 * BannerView 的基础抽象适配器，基于 [RecyclerView.Adapter]。
 * 当你的数据源是静态的、不经常变动，或不需要复杂的更新动画时，推荐继承此类。
 * 它在内部封装了无限循环所需的数据映射逻辑。
 *
 * @param T 数据项的类型。
 * @param VB 视图绑定的类型。
 * @property bindingInflater [ViewBinding] 类的 `inflate` 方法引用，用于自动创建视图。
 *
 * @example
 * ```kotlin
 * // 1. 定义你的 item 布局，例如 `item_image_banner.xml`:
 * // <ImageView xmlns:android="http://schemas.android.com/apk/res/android"
 * //    android:id="@+id/bannerImageView"
 * //    android:layout_width="match_parent"
 * //    android:layout_height="match_parent"
 * //    android:scaleType="centerCrop" />
 *
 * // 2. 创建你的适配器类，继承自 BannerAdapter
 * class ImageBannerAdapter : BannerAdapter<String, ItemImageBannerBinding>(
 *     ItemImageBannerBinding::inflate
 * ) {
 *     // 3. 实现核心的数据绑定逻辑
 *     override fun ItemImageBannerBinding.onBannerBind(realPosition: Int, data: String) {
 *         // 'this' 关键字在这里指向 ItemImageBannerBinding 实例，可以直接访问视图ID。
 *         // 使用你喜欢的图片加载库来加载图片。
 *         Glide.with(root).load(data).into(bannerImageView)
 *     }
 * }
 *
 * // 4. 在你的 Activity 或 Fragment 中使用适配器
 * val imageAdapter = ImageBannerAdapter()
 * bannerView.setAdapter(imageAdapter)
 *
 * // 5. 提交数据列表
 * imageAdapter.submitList(listOf("url_to_image_1.jpg", "url_to_image_2.jpg"))
 * ```
 */
abstract class BannerAdapter<T, VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB
) : RecyclerView.Adapter<BindingViewHolder<VB>>() {

    /**
     * 内部状态标志位。
     * 当数据从可循环状态（N > 1）变为不可循环状态（N <= 1）时，
     * BannerView 会强制将位置重置到 0。此标志位用于在这种情况下屏蔽掉一次不必要的 onPageSelected 回调。
     */
    internal var isResetting = false

    /**
     * 内部持有的真实数据列表。
     * 使用 private var 确保数据只能通过 `submitList` 方法进行修改，实现封装。
     */
    private var realData: List<T> = emptyList()

    /**
     * 内部持有的 item 点击事件监听器。
     * 由 BannerView 统一设置和管理。
     */
    internal var onItemClickListener: ((data: T, realPosition: Int) -> Unit)? = null

    /**
     * 提交新的数据列表到适配器。
     * 此方法会触发 `notifyDataSetChanged()` 进行全量刷新，适用于数据量不大或不常变的场景。
     * @param data 新的数据列表。
     */
    fun submitList(data: List<T>) {
        // 更新内部数据源。
        this.realData = data
        // 通知 RecyclerView 数据集已发生变化，需要重绘。
        notifyDataSetChanged()
    }

    /**
     * (内部使用) 获取真实的原始数据列表。
     */
    internal fun getRealData(): List<T> = realData

    /**
     * (内部使用) 获取真实数据项的数量。
     */
    internal fun getRealCount(): Int = realData.size

    /**
     * (内部使用) 将 Adapter 的虚拟位置（包含头尾的假 item）映射到真实数据列表的位置索引。
     * 这是实现无限循环数据映射的核心。
     * @param position Adapter 中的虚拟位置。
     * @return 在真实数据列表中的位置索引。
     */
    internal fun getRealPosition(position: Int): Int {
        // 如果未启用循环，虚拟位置即真实位置。
        if (!isLoopingEnabled()) return position
        if (getRealCount() == 0) return -1

        return when (position) {
            // 虚拟的头部 (位置0) 映射到真实数据的尾部。
            0 -> getRealCount() - 1
            // 虚拟的尾部 (位置 realCount + 1) 映射到真实数据的头部。
            getRealCount() + 1 -> 0
            // 其他真实 item 的位置需要减 1 才是其在真实数据列表中的索引。
            else -> position - 1
        }
    }

    /**
     * (内部使用) 判断是否应启用无限循环模式。
     * 只有当真实数据项数量大于1时，循环才有意义。
     */
    internal open fun isLoopingEnabled(): Boolean = getRealCount() > 1

    /**
     * (内部使用) 判断给定的虚拟位置是否为“假”的 item（即用于实现无缝跳转的头尾副本）。
     */
    private fun isFakeItem(position: Int): Boolean {
        return isLoopingEnabled() && (position == 0 || position == getRealCount() + 1)
    }

    /**
     * 返回给 RecyclerView 的 item 总数。
     * 在启用循环时，这个数量会是真实数量 + 2。
     */
    override fun getItemCount(): Int {
        // 如果没有数据，返回 0。
        if (getRealCount() == 0) return 0
        // 如果启用循环，则在真实数据的基础上增加头尾两个 item。
        return if (isLoopingEnabled()) getRealCount() + 2 else getRealCount()
    }

    /**
     * 创建 ViewHolder。此方法在 `final` 方法中被调用，确保点击监听器总能被正确设置。
     */
    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<VB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = bindingInflater(inflater, parent, false)
        val holder = BindingViewHolder(binding)

        // 在 ViewHolder 创建时设置点击监听，这是最高效的方式，避免在 onBindViewHolder 中重复创建监听器。
        holder.itemView.setOnClickListener {
            // 使用 bindingAdapterPosition 获取最新的、安全的位置，防止因数据变动导致位置错乱。
            val position = holder.bindingAdapterPosition
            // 校验位置的有效性。
            if (position != RecyclerView.NO_POSITION && getRealCount() > 0) {
                // 将 adapter 的虚拟位置转换为真实数据的索引。
                val realPosition = getRealPosition(position)
                if (realPosition != -1) {
                    // 调用由 BannerView 注入的监听器，并将真实数据和位置传递出去。
                    onItemClickListener?.invoke(realData[realPosition], realPosition)
                }
            }
        }
        return holder
    }

    /**
     * 绑定数据到 ViewHolder。此方法是 `final` 的，它负责分发绑定逻辑。
     */
    final override fun onBindViewHolder(holder: BindingViewHolder<VB>, position: Int) {
        // 判断当前位置是否为“假”的 item。
        if (isFakeItem(position)) {
            // 默认情况下，为“假”的 item 绑定其对应的真实数据，以实现视觉上的无缝衔接。
            // 例如，虚拟头部绑定真实尾部数据。
            val realPosition = getRealPosition(position)
            if (realPosition != -1) {
                holder.binding.onBannerBind(realPosition, realData[realPosition])
            }
        } else {
            // 如果是真实 item，则直接进行绑定。
            val realPosition = getRealPosition(position)
            if (realPosition != -1) {
                // 在 binding 的上下文中调用子类实现的绑定方法。
                holder.binding.onBannerBind(realPosition, realData[realPosition])
            }
        }
    }

    /**
     * 子类必须实现此核心方法，以将真实数据绑定到视图。
     * 这是一个 [ViewBinding] 类的扩展函数，旨在提供最简洁、最符合 Kotlin 习惯的绑定语法。
     * 在实现此方法时，你可以直接访问 `ViewBinding` 中定义的所有视图 ID，无需 `binding.` 前缀。
     *
     * @param realPosition 在真实数据列表中的位置索引 (从 0 开始)。
     * @param data 该位置对应的数据项。
     */
    abstract fun VB.onBannerBind(realPosition: Int, data: T)
}