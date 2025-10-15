package com.demo.core.view.infinite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.demo.core.view.base.BindingViewHolder

/**
 * **【基础版】无限循环适配器**
 *
 * 一个基于 `notifyDataSetChanged()` 的无限循环适配器基类。
 * 它通过返回一个巨大的 itemCount 和使用取模运算来实现无限循环，简单高效。
 * 适用于数据不会频繁变动的场景（如固定的 Banner）。
 *
 * @param T 数据项的类型。
 * @param VB 视图绑定的类型。
 * @property bindingInflater ViewBinding 类的 `inflate` 方法引用。
 *
 * @example
 * // 1. 创建你的 Adapter
 * class MyBannerAdapter : InfiniteAdapter<Banner, ItemBannerBinding>(
 *     ItemBannerBinding::inflate
 * ) {
 *     override fun ItemBannerBinding.onBindRealViewHolder(realPosition: Int, data: Banner) {
 *         bannerTitle.text = data.title
 *     }
 * }
 *
 * // 2. 在 Fragment/Activity 中使用
 * val bannerAdapter = MyBannerAdapter()
 * viewPager.setupInfiniteAdapter(bannerAdapter)
 * bannerAdapter.submitList(bannerList)
 * viewPager.jumpToInfiniteStart()
 *
 * @example
 * val bannerAdapter = MyBannerAdapter()
 *
 * // 【推荐】标准用法，获取真实位置
 * bannerAdapter.onItemClickListener = { banner, realPosition ->
 *     // 处理业务逻辑
 *     viewPager.setCurrentInfiniteItem(realPosition)
 * }
 *
 * // 【可选】高级用法，获取虚拟位置 (adapterPosition)
 * bannerAdapter.onItemClickListenerWithVirtualPosition = { banner, virtualPosition ->
 *     // 用于需要原始 adapter position 的特殊场景
 * }
 */
abstract class InfiniteAdapter<T, VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB
) : RecyclerView.Adapter<BindingViewHolder<VB>>(), IInfiniteAdapter<T> {

    private var data: List<T> = emptyList()

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

    /**
     * 向适配器提交新的数据列表。
     * 此方法会调用 `notifyDataSetChanged()` 来刷新整个列表。
     *
     * @param data 新的数据列表。
     */
    fun submitList(data: List<T>) {
        this.data = data
        notifyDataSetChanged()
    }

    final override fun getItemCount(): Int = if (getRealCount() > 1) Int.MAX_VALUE else getRealCount()

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<VB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = bindingInflater(inflater, parent, false)
        val holder = BindingViewHolder(binding)
        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION && getRealCount() > 0) {
                val realPosition = position % getRealCount()
                val item = data[realPosition]
                onItemClickListener?.invoke(item, realPosition)
                onItemClickListenerWithVirtualPosition?.invoke(item, position)
            }
        }
        return holder
    }

    final override fun onBindViewHolder(holder: BindingViewHolder<VB>, position: Int) {
        if (getRealCount() > 0) {
            val realPosition = position % getRealCount()
            holder.binding.onBindRealViewHolder(realPosition, data[realPosition])
        }
    }

    // --- IInfiniteAdapter 接口实现 ---
    final override fun getRealCount(): Int = data.size
    final override fun getRealItem(realPosition: Int): T? = data.getOrNull(realPosition)
    final override fun getRealData(): List<T> = data

    /**
     * 子类必须实现此方法以将真实数据绑定到视图。
     *
     * @receiver 项目布局的 ViewBinding 实例。
     * @param realPosition 在真实数据列表中的位置。
     * @param data 该位置的数据项。
     */
    abstract fun VB.onBindRealViewHolder(realPosition: Int, data: T)
}
