package com.demo.core.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

/**
 * DSL 作用域限定注解，防止在 DSL 嵌套时方法污染
 */
@DslMarker
annotation class AdapterDsl

/**
 * BaseListAdapter 的 DSL 构建器
 */
@AdapterDsl
class ListAdapterBuilder<T : Any, VB : ViewBinding> {

    // ==========================================
    // 🌟 内部状态变量（使用 Action 后缀避免与公开方法同名冲突）
    // 且完全不需要 @PublishedApi，使用纯粹的 internal 保护封装性
    // ==========================================

    internal var areItemsTheSameAction: ((old: T, new: T) -> Boolean)? = null
    internal var onBindAction: (VB.(item: T, position: Int) -> Unit)? = null

    internal var areContentsTheSameAction: ((old: T, new: T) -> Boolean)? = null
    internal var getChangePayloadAction: ((old: T, new: T) -> Any?)? = null

    internal var onBindPayloadsAction: (VB.(item: T, position: Int, payloads: List<Any>) -> Unit)? =
        null

    internal var onItemClickAction: ((view: View, item: T, position: Int) -> Unit)? = null
    internal var onItemLongClickAction: ((view: View, item: T, position: Int) -> Boolean)? = null

    // ==========================================
    // 🌟 暴露给业务层的公开 DSL 语法糖
    // ==========================================

    /**
     * 语法糖：大多数情况下，比较 Item 是否相同只需比较 ID。
     * 用法: itemDiffId { it.id }
     */
    fun itemDiffId(selector: (T) -> Any?) {
        areItemsTheSameAction = { old, new -> selector(old) == selector(new) }
    }

    /** 原始对比逻辑 */
    fun areItemsTheSame(block: (oldItem: T, newItem: T) -> Boolean) {
        areItemsTheSameAction = block
    }

    fun areContentsTheSame(block: (oldItem: T, newItem: T) -> Boolean) {
        areContentsTheSameAction = block
    }

    fun getChangePayload(block: (oldItem: T, newItem: T) -> Any?) {
        getChangePayloadAction = block
    }

    /**
     * 配置全量绑定逻辑（必填）。
     *
     * 在此闭包中完成 Item 的完整初始化。
     * 注意：此逻辑是局部刷新的最终兜底方案，必须保证其逻辑的完整性。
     */
    fun onBind(block: VB.(item: T, position: Int) -> Unit) {
        onBindAction = block
    }

    /**
     * 配置局部刷新逻辑（可选）。
     *
     * ### 🌟 开发规范示例：
     * ```
     * onBindViewPayloads { item, position, payloads ->
     *     if (payloads.contains("LIKE_CHANGE")) {
     *         // ✅ 仅更新点赞 UI (不调用全量绑定)
     *         tvLikeCount.text = item.likes.toString()
     *     } else {
     *         // 🚨 遇到不认识的标识，必须手动调用 onBindView 执行全量刷新
     *         onBindView(item, position)
     *     }
     * }
     * ```
     */
    fun onBindPayloads(block: VB.(item: T, position: Int, payloads: List<Any>) -> Unit) {
        onBindPayloadsAction = block
    }

    /** 点击事件 (不带 View) */
    fun onClick(block: (item: T, position: Int) -> Unit) {
        onItemClickAction = { _, item, pos -> block(item, pos) }
    }

    /** 点击事件 (带 View) */
    fun onViewClick(block: (view: View, item: T, position: Int) -> Unit) {
        onItemClickAction = block
    }

    /** 长按事件 */
    fun onLongClick(block: (item: T, position: Int) -> Boolean) {
        onItemLongClickAction = { _, item, pos -> block(item, pos) }
    }
}

/**
 * 🌟 核心入口：动态构建 BaseListAdapter 的顶层工厂函数
 * (移除了 inline，因为 Adapter 创建是极低频操作，不 inline 能带来更好的 API 封装性)
 *
 * @param bindingInflater ViewBinding 的 inflate 方法，如 `ItemUserBinding::inflate`
 * @param setup DSL 配置块
 */
fun <T : Any, VB : ViewBinding> buildListAdapter(
    bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB,
    setup: ListAdapterBuilder<T, VB>.() -> Unit
): BaseListAdapter<T, VB> {

    val builder = ListAdapterBuilder<T, VB>().apply(setup)

    // 必填项校验
    requireNotNull(builder.areItemsTheSameAction) { "必须在 DSL 中配置 itemDiffId 或 areItemsTheSame" }
    requireNotNull(builder.onBindAction) { "必须在 DSL 中配置 onBind" }

    // 返回匿名内部类实例
    return object : BaseListAdapter<T, VB>(bindingInflater) {

        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
            builder.areItemsTheSameAction!!.invoke(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
            builder.areContentsTheSameAction?.invoke(oldItem, newItem)
                ?: super.areContentsTheSame(oldItem, newItem)

        override fun getChangePayload(oldItem: T, newItem: T): Any? =
            builder.getChangePayloadAction?.invoke(oldItem, newItem)
                ?: super.getChangePayload(oldItem, newItem)

        // 注意这里的 invoke(this, item, position) 语法：
        // 第一个参数 this 指代的是 VB 实例，这是因为 onBindAction 是一个带接收者的函数类型 (VB.() -> Unit)
        override fun VB.onBindView(item: T, position: Int) {
            builder.onBindAction!!.invoke(this, item, position)
        }

        override fun VB.onBindView(item: T, position: Int, payloads: List<Any>) {
            if (builder.onBindPayloadsAction != null) {
                builder.onBindPayloadsAction!!.invoke(this, item, position, payloads)
            } else {
                // 安全降级：如果没有在 DSL 中配置 onBindPayloads，则执行全量刷新
                onBindView(item, position)
            }
        }
    }.apply {
        // 配置事件监听
        onItemClickListener = builder.onItemClickAction
        onItemLongClickListener = builder.onItemLongClickAction
    }
}