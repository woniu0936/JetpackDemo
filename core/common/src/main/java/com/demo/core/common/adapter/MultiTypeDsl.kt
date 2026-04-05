package com.demo.core.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

@DslMarker
annotation class MultiTypeDsl

/**
 * 专为 MultiTypeAdapter 设计的 Binder 构建器
 */
@MultiTypeDsl
class ItemBinderBuilder<T : Any, VB : ViewBinding> {

    // 内部存储 Action
    @PublishedApi
    internal var areItemsTheSameAction: ((old: T, new: T) -> Boolean)? = null
    @PublishedApi
    internal var areContentsTheSameAction: ((old: T, new: T) -> Boolean)? = null
    @PublishedApi
    internal var getChangePayloadAction: ((old: T, new: T) -> Any?)? = null

    // 绑定逻辑 Action
    @PublishedApi
    internal var onBindViewAction: (VB.(item: T) -> Unit)? = null
    @PublishedApi
    internal var onBindViewPayloadsAction: (VB.(item: T, payloads: List<Any>) -> Unit)? = null

    // 事件监听 Action (严格对齐你的 ItemBinder 签名)
    @PublishedApi
    internal var onItemClickAction: ((position: Int, item: T) -> Unit)? = null
    @PublishedApi
    internal var onItemLongClickAction: ((position: Int, item: T) -> Unit)? = null

    // ==========================================
    // 🌟 DSL 语法糖方法
    // ==========================================

    fun itemDiffId(selector: (T) -> Any?) {
        areItemsTheSameAction = { old, new -> selector(old) == selector(new) }
    }

    fun areItemsTheSame(block: (oldItem: T, newItem: T) -> Boolean) {
        areItemsTheSameAction = block
    }

    fun areContentsTheSame(block: (oldItem: T, newItem: T) -> Boolean) {
        areContentsTheSameAction = block
    }

    fun getChangePayload(block: (oldItem: T, newItem: T) -> Any?) {
        getChangePayloadAction = block
    }

    /** 配置全量绑定 */
    fun onBindView(block: VB.(item: T) -> Unit) {
        onBindViewAction = block
    }

    /** 配置局部绑定 */
    fun onBindViewPayloads(block: VB.(item: T, payloads: List<Any>) -> Unit) {
        onBindViewPayloadsAction = block
    }

    /**
     * 点击事件
     * @param block (position: Int, item: T)
     */
    fun onClick(block: (position: Int, item: T) -> Unit) {
        onItemClickAction = block
    }

    /**
     * 长按事件
     * @param block (position: Int, item: T)
     */
    fun onLongClick(block: (position: Int, item: T) -> Unit) {
        onItemLongClickAction = block
    }
}

/**
 * 🌟 核心注册扩展函数
 *
 * 1. inline + reified T: 为了在内部调用 this.register(binder) 时能保留 T 的真实类型进行哈希映射。
 * 2. noinline setup: 阻止编译器将 DSL 业务代码复制到调用方，完美防止字节码膨胀！
 */
inline fun <reified T : Any, VB : ViewBinding> MultiTypeAdapter.register(
    noinline bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB,
    noinline setup: ItemBinderBuilder<T, VB>.() -> Unit
): MultiTypeAdapter {

    val builder = ItemBinderBuilder<T, VB>().apply(setup)

    requireNotNull(builder.areItemsTheSameAction) { "必须在 DSL 中配置 itemDiffId 或 areItemsTheSame" }
    requireNotNull(builder.onBindViewAction) { "必须在 DSL 中配置 onBindView" }

    // 动态创建匿名的 ItemBinder 实例
    val binder = object : ItemBinder<T, VB>(bindingInflater) {

        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
            builder.areItemsTheSameAction!!.invoke(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
            builder.areContentsTheSameAction?.invoke(oldItem, newItem) ?: super.areContentsTheSame(oldItem, newItem)

        override fun getChangePayload(oldItem: T, newItem: T): Any? =
            builder.getChangePayloadAction?.invoke(oldItem, newItem) ?: super.getChangePayload(oldItem, newItem)

        // 重写成员扩展函数
        override fun VB.onBindView(item: T) {
            builder.onBindViewAction!!.invoke(this, item)
        }

        // 重写带 Payload 的成员扩展函数
        override fun VB.onBindViewPayloads(item: T, payloads: List<Any>) {
            if (builder.onBindViewPayloadsAction != null) {
                builder.onBindViewPayloadsAction!!.invoke(this, item, payloads)
            } else {
                onBindView(item) // 安全降级为全量刷新
            }
        }
    }.apply {
        // 配置你在 ItemBinder 里定义的点击事件属性
        onItemClick = builder.onItemClickAction
        onLongClick = builder.onItemLongClickAction
    }

    // 调用 MultiTypeAdapter 核心类的原始 register 方法完成绑定
    return this.register(binder)
}