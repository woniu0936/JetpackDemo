package com.demo.core.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

/**
 * BaseAdapter 的 DSL 构建器
 */
@AdapterDsl
class AdapterBuilder<T, VB : ViewBinding> {

    internal var onBindAction: (VB.(item: T, position: Int) -> Unit)? = null
    internal var onBindPayloadsAction: (VB.(item: T, position: Int, payloads: List<Any>) -> Unit)? = null
    internal var onItemClickAction: ((view: View, item: T, position: Int) -> Unit)? = null
    internal var onItemLongClickAction: ((view: View, item: T, position: Int) -> Boolean)? = null

    /**
     * 配置全量绑定逻辑（必填）。
     */
    fun onBind(block: VB.(item: T, position: Int) -> Unit) {
        onBindAction = block
    }

    /**
     * 配置局部刷新逻辑（可选）。
     */
    fun onBindPayloads(block: VB.(item: T, position: Int, payloads: List<Any>) -> Unit) {
        onBindPayloadsAction = block
    }

    /** 点击事件 (不带 View) */
    fun onClick(block: (item: T, position: Int) -> Unit) {
        onItemClickAction = { _, item, pos -> block(item, pos) }
    }

    /** 长按事件 */
    fun onLongClick(block: (item: T, position: Int) -> Boolean) {
        onItemLongClickAction = { _, item, pos -> block(item, pos) }
    }
}

/**
 * 🌟 核心入口：动态构建 BaseAdapter 的顶层工厂函数。
 *
 * @param bindingInflater ViewBinding 的 inflate 方法，如 `ItemUserBinding::inflate`
 * @param setup DSL 配置块
 *
 * @example
 * val adapter = buildAdapter<RepoInfo, RepoItemBinding>(RepoItemBinding::inflate) {
 *     onBind { item, _ ->
 *         nameText.text = item.name
 *         descriptionText.text = item.description
 *         starCountText.text = "★ ${item.stars}"
 *         checkbox.isChecked = item.selected
 *     }
 *
 *     onBindPayloads { item, _, payloads ->
 *         when {
 *             payloads.contains("PAYLOAD_SELECTION") -> checkbox.isChecked = item.selected
 *             payloads.contains("PAYLOAD_STARS") -> starCountText.text = "★ ${item.stars}"
 *             else -> onBind(item, 0)
 *         }
 *     }
 *
 *     onClick { item, _ ->
 *         val next = adapter.getCurrentList().map { repo ->
 *             if (repo.id == item.id) repo.copy(stars = repo.stars + 1, selected = !repo.selected)
 *             else repo
 *         }
 *         adapter.setData(next)
 *     }
 * }
 * recyclerView.adapter = adapter
 * adapter.setData(sampleRepos())
 */
fun <T, VB : ViewBinding> buildAdapter(
    bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB,
    setup: AdapterBuilder<T, VB>.() -> Unit
): BaseAdapter<T, VB> {

    val builder = AdapterBuilder<T, VB>().apply(setup)
    requireNotNull(builder.onBindAction) { "必须在 DSL 中配置 onBind" }

    return object : BaseAdapter<T, VB>(bindingInflater) {

        override fun VB.onBindView(item: T, position: Int) {
            builder.onBindAction!!.invoke(this, item, position)
        }

        override fun VB.onBindView(item: T, position: Int, payloads: List<Any>) {
            if (builder.onBindPayloadsAction != null) {
                builder.onBindPayloadsAction!!.invoke(this, item, position, payloads)
            } else {
                onBindView(item, position)
            }
        }
    }.apply {
        onItemClickListener = builder.onItemClickAction
        onItemLongClickListener = builder.onItemLongClickAction
    }
}
