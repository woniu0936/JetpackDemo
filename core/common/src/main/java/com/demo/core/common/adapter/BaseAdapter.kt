package com.demo.core.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.demo.core.common.utils.ensureOnMainThread

/**
 * 一个商业级的、健壮的 RecyclerView.Adapter 基类，专为 ViewBinding 设计。
 *
 * 【设计意图】
 * 本 Adapter 专为不使用 DiffUtil 的简单场景设计，例如数据源稳定、一次性加载且不会
 * 频繁局部变动的列表。它通过 `notifyDataSetChanged()` 进行全局刷新。
 *
 * 如果你的列表数据会频繁更新，强烈建议使用 `BaseListAdapter` 以获得最佳性能。
 *
 * @param T 数据模型的类型。
 * @param VB ViewBinding 类的类型。
 * @property bindingInflater 用于创建 ViewBinding 实例的 inflate 方法引用。
 *
 * @example
 * class StaticMenuAdapter : BaseAdapter<MenuItem, ItemMenuBinding>(ItemMenuBinding::inflate) {
 *     override fun ItemMenuBinding.onBindView(item: MenuItem, position: Int) {
 *         menuTitle.text = item.title
 *     }
 * }
 */
abstract class BaseAdapter<T, VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB
) : RecyclerView.Adapter<BaseAdapter.BaseViewHolder<VB>>() {

    private var dataList: List<T> = emptyList()

    /**
     * Item 点击事件的回调监听器。
     * lambda 参数: view: View, item: T, position: Int
     */
    var onItemClickListener: ((View, T, Int) -> Unit)? = null
    /**
     * Item 长按事件的回调监听器。
     * lambda 参数: view: View, item: T, position: Int
     * lambda 返回值: Boolean, `true` 表示事件已被消费。
     */
    var onItemLongClickListener: ((View, T, Int) -> Boolean)? = null

    /**
     * 【核心】子类必须实现此方法，以定义如何将数据绑定到视图。
     * @receiver ViewBinding 实例，可以直接访问视图控件。
     * @param item 当前位置的数据模型。
     * @param position 当前项的位置。
     */
    abstract fun VB.onBindView(item: T, position: Int)

    /**
     * (可选) 子类可以重写此方法，以处理通过 payloads 触发的局部刷新。
     */
    open fun VB.onBindView(item: T, position: Int, payloads: List<Any>) {
        onBindView(item, position)
    }

    /**
     * 替换适配器中的数据集并执行全局刷新。
     * 【线程安全】此方法包含运行时检查，强制要求在主线程调用。
     *
     * @param newData 新的数据列表。
     * @throws IllegalStateException 如果不在主线程调用。
     */
    @MainThread
    fun setData(newData: List<T>) {
        ensureOnMainThread("setData")
        this.dataList = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = bindingInflater.invoke(LayoutInflater.from(parent.context), parent, false)
        val holder = BaseViewHolder(binding)
        setupClickListeners(holder)
        return holder
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        getItem(position)?.let { item ->
            holder.binding.onBindView(item, position)
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<VB>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            getItem(position)?.let { item ->
                holder.binding.onBindView(item, position, payloads)
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    /**
     * 安全地获取指定位置的数据项。
     * @return 如果位置有效，返回数据项；否则返回 null。
     */
    fun getItem(position: Int): T? = dataList.getOrNull(position)

    /**
     * 获取当前数据列表的防御性副本。
     * @return 一个包含当前所有数据项的全新列表，对它的修改不会影响 Adapter 内部。
     */
    fun getCurrentList(): List<T> = dataList.toList()

    private fun setupClickListeners(holder: BaseViewHolder<VB>) {
        holder.binding.root.setOnClickListener { view ->
            handleItemClick(view, holder.bindingAdapterPosition)
        }

        holder.binding.root.setOnLongClickListener { view ->
            handleItemLongClick(view, holder.bindingAdapterPosition)
        }
    }

    private fun handleItemClick(view: View, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            getItem(position)?.let { item ->
                onItemClickListener?.invoke(view, item, position)
            }
        }
    }

    private fun handleItemLongClick(view: View, position: Int): Boolean {
        if (position != RecyclerView.NO_POSITION) {
            getItem(position)?.let { item ->
                return onItemLongClickListener?.invoke(view, item, position) ?: false
            }
        }
        return false
    }

    class BaseViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)
}