package com.demo.core.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.demo.core.common.utils.ensureOnMainThread

/**
 * 一个商业级的、高性能的 ListAdapter 基类，专为 ViewBinding 设计。
 *
 * 【设计意图】
 * 本 Adapter 内置了 DiffUtil 支持，是处理动态、可变列表数据的首选方案。
 * 它能够自动处理后台线程的差异计算和主线程的UI更新，提供流畅的动画和最佳性能。
 *
 * @param T 数据模型的类型。
 * @param VB ViewBinding 类的类型。
 * @param bindingInflater 用于创建 ViewBinding 实例的 inflate 方法引用。
 *                        例如: `ItemMyDataBinding::inflate`
 * @param diffCallback 用于计算数据差异的 DiffUtil.ItemCallback 实例。
 *
 * @example
 * // 1. 创建 DiffUtil
 * class MyDataDiff : DiffUtil.ItemCallback<MyData>() { ... }
 *
 * // 2. 创建 Adapter
 * class MyAdapter : BaseListAdapter<MyData, ItemMyDataBinding>(
 *     ItemMyDataBinding::inflate,
 *     MyDataDiff()
 * ) {
 *     override fun ItemMyDataBinding.onBindView(item: MyData, position: Int) {
 *         titleTextView.text = item.title
 *     }
 * }
 */
abstract class BaseListAdapter<T, VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB,
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseListAdapter.BaseViewHolder<VB>>(diffCallback) {

    /**
     * Item 点击事件的回调监听器。
     */
    var onItemClickListener: ((View, T, Int) -> Unit)? = null
    /**
     * Item 长按事件的回调监听器。
     */
    var onItemLongClickListener: ((View, T, Int) -> Boolean)? = null

    /**
     * 【核心】子类必须实现此方法，以定义如何将数据绑定到视图。
     */
    abstract fun VB.onBindView(item: T, position: Int)

    /**
     * (可选) 子类可以重写此方法，以处理局部刷新。
     */
    open fun VB.onBindView(item: T, position: Int, payloads: List<Any>) {
        onBindView(item, position)
    }

    /**
     * 提交新的数据列表。ListAdapter 会在后台线程计算差异并更新UI。
     * 【线程安全】ListAdapter 内部已处理线程问题，但推荐在主线程提交。
     * 我们增加一层检查以规范团队行为。
     *
     * @param list 新的数据列表。
     */
    @MainThread
    override fun submitList(list: List<T>?) {
        ensureOnMainThread("submitList")
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = bindingInflater.invoke(LayoutInflater.from(parent.context), parent, false)
        val holder = BaseViewHolder(binding)
        setupClickListeners(holder)
        return holder
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        // getItem 是 ListAdapter 的方法，内部有边界检查
        holder.binding.onBindView(getItem(position), position)
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<VB>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.binding.onBindView(getItem(position), position, payloads)
        }
    }

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
            onItemClickListener?.invoke(view, getItem(position), position)
        }
    }

    private fun handleItemLongClick(view: View, position: Int): Boolean {
        if (position != RecyclerView.NO_POSITION) {
            return onItemLongClickListener?.invoke(view, getItem(position), position) ?: false
        }
        return false
    }

    class BaseViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)
}