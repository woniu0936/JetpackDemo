package com.demo.core.common.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 一个商业级的、高性能的 ListAdapter 基类，专为 ViewBinding 设计。
 *
 * 【设计意图】
 * 1. 专为单一数据类型的列表设计。
 * 2. 深度整合 DiffUtil，自动处理后台线程的差异计算和主线程的 UI 更新，提供流畅的默认动画。
 * 3. 采用 DSL (扩展函数) 风格的绑定设计，彻底消除手写 ViewHolder 和 `binding.` 前缀的样板代码。
 * 4. Diff 逻辑被内聚到 Adapter 自身实现中，告别繁琐的外部 DiffCallback 声明。
 *
 * @param T  数据模型的类型 (必须声明为非空上限，如 T : Any)。
 * @param VB ViewBinding 类的类型。
 * @param bindingInflater 用于创建 ViewBinding 实例的 inflate 方法引用 (如 `ItemUserBinding::inflate`)。
 *
 * @example
 * // 极简实现示例
 * class UserAdapter : BaseListAdapter<UserModel, ItemUserBinding>(ItemUserBinding::inflate) {
 *
 *     // 1. 实现 Diff 对比规则 (必备)
 *     override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel) = oldItem.id == newItem.id
 *
 *     // (可选) 如果只是某个字段变化，可以提供 Payload 标识
 *     override fun getChangePayload(oldItem: UserModel, newItem: UserModel): Any? {
 *         if (oldItem.age != newItem.age) return "UPDATE_AGE"
 *         return null
 *     }
 *
 *     // 2. 全量刷新 UI 绑定 (必备)
 *     // 这里的 this 作用域已经是 ItemUserBinding
 *     override fun ItemUserBinding.onBind(item: UserModel, position: Int) {
 *         tvName.text = item.name
 *         tvAge.text = item.age.toString()
 *     }
 *
 *     // 3. 局部刷新 UI 绑定 (可选)
 *     override fun ItemUserBinding.onBind(item: UserModel, position: Int, payloads: List<Any>) {
 *         if (payloads.contains("UPDATE_AGE")) {
 *             tvAge.text = item.age.toString()
 *         } else {
 *             super.onBind(item, position, payloads) // 未知 payload 时优雅降级为全量刷新
 *         }
 *     }
 * }
 */
abstract class BaseListAdapter<T : Any, VB : ViewBinding> private constructor(
    private val bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB,
    // 通过构造函数默认参数持有 Callback 引用，避免访问 ListAdapter 内部私有的 mDiffer
    private val diffCallback: BaseListDiffCallback<T>
) : ListAdapter<T, BaseListAdapter.BaseViewHolder<VB>>(diffCallback) {

    constructor(bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB) :
            this(bindingInflater, BaseListDiffCallback<T>())

    init {
        // 像 MultiTypeAdapter 一样，在初始化时建立双向绑定
        diffCallback.adapter = this
    }

    /**
     * Item 点击事件的回调监听器。
     */
    var onItemClickListener: ((View, T, Int) -> Unit)? = null

    /**
     * Item 长按事件的回调监听器。
     */
    var onItemLongClickListener: ((View, T, Int) -> Boolean)? = null


    abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean

    @SuppressLint("DiffUtilEquals")
    open fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem

    open fun getChangePayload(oldItem: T, newItem: T): Any? = null


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
    /**
     * 内部 Diff 代理类，将计算逻辑转发给 Adapter。
     * 这样子类就不需要每次都去 new 一个 DiffUtil.ItemCallback 了。
     */
    class BaseListDiffCallback<T : Any> : DiffUtil.ItemCallback<T>() {

        // 使用 internal 限制访问权限，但类型本身必须对 BaseListAdapter 公开
        internal lateinit var adapter: BaseListAdapter<T, *>

        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
            adapter.areItemsTheSame(oldItem, newItem)

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
            adapter.areContentsTheSame(oldItem, newItem)

        override fun getChangePayload(oldItem: T, newItem: T): Any? =
            adapter.getChangePayload(oldItem, newItem)
    }

}



