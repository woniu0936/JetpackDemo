package com.demo.core.common.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class ItemBinder<T : Any, VB : ViewBinding>(
    val inflate: (LayoutInflater, ViewGroup, Boolean) -> VB
) {

    // 点击回调（业务层注册时设置，类型安全）
    var onItemClick: ((position: Int, item: T) -> Unit)? = null
    var onLongClick: ((position: Int, item: T) -> Unit)? = null

    // 强制实现：判断是否为同一个实体（如：对比 ID）
    abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean

    // 可选实现：判断内容是否相同（Kotlin data class 默认适用）
    @SuppressLint("DiffUtilEquals")
    open fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem

    // 可选实现：用于生成局部刷新的 Payload 标识
    open fun getChangePayload(oldItem: T, newItem: T): Any? = null

    // 强制实现：全量数据绑定
    abstract fun VB.onBindView(item: T)

    // 可选实现：局部数据绑定
    // 默认行为：如果不重写，或遇到未知的 payload，默认降级为全量刷新！安全且健壮！
    open fun VB.onBindViewPayloads(item: T, payloads: List<Any>) {
        // 这里的 this 隐式指向了 B (ViewBinding)
        // 完美调用上面的 onBind 实现降级
        onBindView(item)
    }

    internal open fun setupItemClicks(holder: MultiTypeAdapter.BindingViewHolder<VB>) {
        val root = holder.binding.root

        root.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                // 通过 Adapter 获取最新 item（类型安全由调用方保证）
                @Suppress("UNCHECKED_CAST")
                val item = (holder.bindingAdapter as? MultiTypeAdapter)?.getItemSafely(position) as? T
                item?.let { onItemClick?.invoke(position, it) }
            }
        }

        root.setOnLongClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                @Suppress("UNCHECKED_CAST")
                val item = (holder.bindingAdapter as? MultiTypeAdapter)?.getItemSafely(position) as? T
                item?.let { onLongClick?.invoke(position, it) }
            }
            true
        }
    }

    // 魔法优化点：提供给 Adapter 调用的操作符重载
    // internal 保证了对外面的业务层不可见，非常干净
    internal operator fun invoke(binding: VB, item: T) {
        binding.onBindView(item) // 内部自动拥有了 this 作用域
    }

    internal operator fun invoke(binding: VB, item: T, payloads: List<Any>) {
        binding.onBindViewPayloads(item, payloads)
    }
}