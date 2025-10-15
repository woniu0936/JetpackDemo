package com.demo.core.view.banner.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.demo.core.view.base.BindingViewHolder

/**
 * [BannerView] 的高性能抽象适配器，基于 [ListAdapter]。
 * 当你的数据源是动态的、可变的，并且你希望获得平滑的更新动画时，强烈推荐使用此类。
 * 它利用 [DiffUtil] 在后台线程计算数据差异，实现高效的局部刷新。
 *
 * @param T 数据项的类型，必须拥有稳定的 ID 以供 [DiffUtil] 比较。
 * @param VB 视图绑定的类型。
 * @param diffCallback [DiffUtil.ItemCallback] 的实现，用于定义新旧数据的比较逻辑。
 * @property bindingInflater [ViewBinding] 类的 `inflate` 方法引用。
 *
 * @example
 * ```kotlin
 * // 1. 定义你的数据类
 * data class BannerItem(val id: String, val imageUrl: String)
 *
 * // 2. 创建一个 DiffUtil.ItemCallback
 * object BannerDiffCallback : DiffUtil.ItemCallback<BannerItem>() {
 *     // 判断两个 item 是否是同一个对象（通常通过 ID 比较）
 *     override fun areItemsTheSame(oldItem: BannerItem, newItem: BannerItem): Boolean {
 *         return oldItem.id == newItem.id
 *     }
 *     // 判断两个 item 的内容是否完全相同
 *     override fun areContentsTheSame(oldItem: BannerItem, newItem: BannerItem): Boolean {
 *         return oldItem == newItem
 *     }
 * }
 *
 * // 3. 创建适配器，继承自 BannerListAdapter
 * class ImageBannerListAdapter : BannerListAdapter<BannerItem, ItemBannerImageBinding>(
 *     BannerDiffCallback,
 *     ItemBannerImageBinding::inflate
 * ) {
 *     // 4. 实现核心的数据绑定逻辑
 *     override fun ItemBannerImageBinding.onBannerBind(realPosition: Int, data: BannerItem) {
 *         Glide.with(root).load(data.imageUrl).into(bannerImageView)
 *     }
 * }
 *
 * // 5. 在 Activity/Fragment 中使用
 * val listAdapter = ImageBannerListAdapter()
 * bannerView.setAdapter(listAdapter)
 * // 首次提交数据
 * listAdapter.submitList(listOf(BannerItem("1", "url1"), BannerItem("2", "url2")))
 * // 后续更新数据，会自动计算差异并播放动画
 * listAdapter.submitList(listOf(BannerItem("1", "url1_new"), BannerItem("3", "url3")))
 * ```
 */
abstract class BannerListAdapter<T, VB : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<T>,
    private val bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB
) : ListAdapter<T, BindingViewHolder<VB>>(diffCallback) {

    // 内部状态标志位，用于在数据安全重置时屏蔽不必要的回调。
    internal var isResetting = false
    // 内部持有的 item 点击事件监听器。
    internal var onItemClickListener: ((data: T, realPosition: Int) -> Unit)? = null

    /**
     * (内部使用) 获取当前的真实数据列表。
     * 对于 ListAdapter，数据源是 `currentList`。
     */
    internal fun getRealData(): List<T> = currentList

    /**
     * (内部使用) 获取真实数据项的数量。
     */
    internal fun getRealCount(): Int = currentList.size

    /**
     * (内部使用) 将 Adapter 的虚拟位置映射到真实数据列表的位置索引。
     */
    internal fun getRealPosition(position: Int): Int {
        // 如果未启用循环，虚拟位置即真实位置。
        if (!isLoopingEnabled()) return position
        if (getRealCount() == 0) return -1

        return when (position) {
            0 -> getRealCount() - 1
            getRealCount() + 1 -> 0
            else -> position - 1
        }
    }

    /**
     * (内部使用) 判断是否应启用无限循环模式。
     */
    internal open fun isLoopingEnabled(): Boolean = getRealCount() > 1

    /**
     * (内部使用) 判断给定的虚拟位置是否为“假”的 item。
     */
    private fun isFakeItem(position: Int): Boolean {
        return isLoopingEnabled() && (position == 0 || position == getRealCount() + 1)
    }

    override fun getItemCount(): Int {
        if (getRealCount() == 0) return 0
        return if (isLoopingEnabled()) getRealCount() + 2 else getRealCount()
    }

    /**
     * 创建 ViewHolder。
     * **[修复]** 将此方法标记为 `final`，这样通过 `object : BannerListAdapter(...)` 创建匿名内部类时，
     * 就不再需要重复实现这个方法，从而解决了编译错误。
     */
    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<VB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = bindingInflater(inflater, parent, false)
        val holder = BindingViewHolder(binding)

        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION && getRealCount() > 0) {
                val realPosition = getRealPosition(position)
                if (realPosition != -1) {
                    // ListAdapter 通过 getItem(realPosition) 获取数据更安全，因为它总是代表最新状态。
                    onItemClickListener?.invoke(getItem(realPosition), realPosition)
                }
            }
        }
        return holder
    }

    /**
     * 绑定数据到 ViewHolder。
     * **[修复]** 将此方法标记为 `final`，以解决匿名内部类实现的编译错误。
     * 它负责分发绑定逻辑到子类实现的 `onBannerBind` 方法。
     */
    final override fun onBindViewHolder(holder: BindingViewHolder<VB>, position: Int) {
        // 将虚拟位置转换为真实位置
        val realPosition = getRealPosition(position)
        if (realPosition != -1) {
            // 通过 getItem(realPosition) 获取对应位置的数据项
            holder.binding.onBannerBind(realPosition, getItem(realPosition))
        }
    }

    /**
     * 子类必须实现此核心方法，以将真实数据绑定到视图。
     * 这是一个 [ViewBinding] 类的扩展函数，旨在提供最简洁、最符合 Kotlin 习惯的绑定语法。
     *
     * @param realPosition 在真实数据列表中的位置索引 (从 0 开始)。
     * @param data 该位置对应的数据项。
     */
    abstract fun VB.onBannerBind(realPosition: Int, data: T)
}
