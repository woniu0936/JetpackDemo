package com.demo.core.view.infinite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.demo.core.view.base.BindingViewHolder

/**
 * **【高性能版】无限循环列表适配器**
 *
 * 一个基于 `ListAdapter` 和 `DiffUtil` 的高性能无限循环适配器基类。
 * 它同样通过返回巨大 itemCount 和取模运算实现无限循环，并利用 `DiffUtil` 进行高效的局部刷新。
 * 适用于数据会频繁变动、需要局部刷新和优雅动画的场景。
 *
 * @param T 数据项的类型。
 * @param VB 视图绑定的类型。
 * @param diffCallback 用于计算数据差异的 `DiffUtil.ItemCallback` 实例。
 * @param bindingInflater ViewBinding 类的 `inflate` 方法引用。
 *
 * @example
 * // 1. 创建 DiffUtil.ItemCallback
 * class FeedDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
 *     override fun areItemsTheSame(old: FeedItem, new: FeedItem) = old.id == new.id
 *     override fun areContentsTheSame(old: FeedItem, new: FeedItem) = old == new
 * }
 *
 * // 2. 创建你的 Adapter
 * class FeedAdapter : InfiniteListAdapter<FeedItem, ItemFeedBinding>(
 *     ItemFeedBinding::inflate,
 *     FeedDiffCallback()
 * ) {
 *     override fun onBindRealViewHolder(binding: ItemFeedBinding, realPosition: Int, data: FeedItem) {
 *         binding.feedTitle.text = data.title
 *     }
 * }
 *
 * val feedAdapter = FeedAdapter()
 *
 * @example
 *
 * // 【推荐】标准用法，获取真实位置
 * feedAdapter.onItemClickListener = { feed, realPosition ->
 *     // ...
 * }
 *
 * // 【可选】高级用法，获取虚拟位置 (adapterPosition)
 * feedAdapter.onItemClickListenerWithVirtualPosition = { feed, virtualPosition ->
 *     // ...
 * }
 */
abstract class InfiniteListAdapter<T, VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB,
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BindingViewHolder<VB>>(diffCallback), IInfiniteAdapter<T> {

    /**
     * Item 点击事件的回调监听器。
     * 提供被点击的数据项及其在真实列表中的位置。
     */
    var onItemClickListener: ((data: T, realPosition: Int) -> Unit)? = null

    /**
     * 【高级】Item 点击事件的回调监听器。
     * 提供被点击的数据项及其 **虚拟** 位置 (`bindingAdapterPosition`)。
     * 仅在确实需要原始 adapter position 的特殊场景下使用。
     */
    var onItemClickListenerWithVirtualPosition: ((data: T, virtualPosition: Int) -> Unit)? = null

    // 重写 ListAdapter 的关键方法以注入无限循环逻辑
    final override fun getItemCount(): Int = if (getRealCount() > 1) Int.MAX_VALUE else getRealCount()

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<VB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = bindingInflater(inflater, parent, false)
        val holder = BindingViewHolder(binding)
        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION && getRealCount() > 0) {
                val realPosition = position % getRealCount()
                val item = getItem(realPosition)
                onItemClickListener?.invoke(item, realPosition)
                onItemClickListenerWithVirtualPosition?.invoke(item, position)
            }
        }
        return holder
    }

    final override fun onBindViewHolder(holder: BindingViewHolder<VB>, position: Int) {
        if (getRealCount() > 0) {
            val realPosition = position % getRealCount()
            onBindRealViewHolder(holder.binding, realPosition, getItem(realPosition))
        }
    }

    // --- IInfiniteAdapter 接口实现 ---
    final override fun getRealCount(): Int = currentList.size
    final override fun getRealItem(realPosition: Int): T? = currentList.getOrNull(realPosition)
    final override fun getRealData(): List<T> = currentList

    /**
     * 子类必须实现此方法以将真实数据绑定到视图。
     *
     * @param binding 项目布局的 ViewBinding 实例。
     * @param realPosition 在真实数据列表中的位置。
     * @param data 该位置的数据项。
     */
    abstract fun onBindRealViewHolder(binding: VB, realPosition: Int, data: T)
}
