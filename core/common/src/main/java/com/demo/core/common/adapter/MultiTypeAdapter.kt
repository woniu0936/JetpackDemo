package com.demo.core.common.adapter

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 一个商业级的、高度可扩展的多类型列表适配器 (MultiTypeAdapter)。
 *
 * 【设计意图】
 * 1. 专门用于处理异构数据列表（同一列表中展示多种完全不同的 UI 样式）。
 * 2. 采用“委托模式 (Delegate Pattern)”，将每种数据类型的创建、绑定和差异计算逻辑抽离到独立的 [ItemBinder] 中。
 * 3. 核心算法：使用 `Class.name.hashCode()` 自动生成 ViewType，并内置哈希冲突检测，彻底告别手动维护 ViewType 常量的历史。
 * 4. 深度整合 ListAdapter：将全局的 DiffUtil 计算请求智能路由至对应类型的 [ItemBinder] 处理。
 * 5. 极致简洁的注册 API：支持流式调用，利用 Kotlin 扩展函数消除样板代码。
 *
 * 【核心机制】
 * - 注册：通过 `.register(SomeBinder())` 将数据模型类与处理逻辑绑定。
 * - 路由：Adapter 根据数据的 Class 自动查找并调用对应的 Binder。
 * - 性能：继承自 ListAdapter，享有原生异步 Diff 差量刷新能力。
 *
 * @example
 * // 1. 定义多种数据模型 (无需实现任何接口)
 * data class TextItem(val id: String, val text: String)
 * data class ImageItem(val id: String, val url: String)
 *
 * // 2. 为每种模型定义 Binder
 * class TextBinder : ItemBinder<TextItem, ItemTextBinding>(ItemTextBinding::inflate) {
 *     override fun areItemsTheSame(old: TextItem, new: TextItem) = old.id == new.id
 *     override fun ItemTextBinding.onBind(item: TextItem) {
 *         tvContent.text = item.text
 *     }
 * }
 *
 * // 3. 在 Activity/Fragment 中初始化与使用
 * val adapter = MultiTypeAdapter()
 *     .register(TextBinder())
 *     .register(ImageBinder())
 *     .register(AdBinder()) // 轻松扩展更多类型
 *
 * recyclerView.adapter = adapter
 *
 * // 4. 提交混合数据列表
 * val items = listOf(
 *     TextItem("1", "这是文字"),
 *     ImageItem("2", "https://..."),
 *     TextItem("3", "另一条文字")
 * )
 * adapter.submitList(items)
 */
class MultiTypeAdapter private constructor(
    private val diffCallback: MultiTypeDiffCallback = MultiTypeDiffCallback()
)  : ListAdapter<Any, MultiTypeAdapter.BindingViewHolder<ViewBinding>>(diffCallback) {

    init {
        // 绑定引用，供 DiffUtil 路由使用
        diffCallback.adapter = this
    }

    @PublishedApi
    internal val binders = SparseArray<ItemBinder<Any, ViewBinding>>()

    @PublishedApi
    internal val classToType = HashMap<Class<*>, Int>()

    /**
     * 极简注册 API：绑定数据类与对应的 ItemBinder
     * 支持链式调用
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any, B : ViewBinding> register(binder: ItemBinder<T, B>): MultiTypeAdapter {
        val clazz = T::class.java
        // 核心算法：类名 Hash 转换成 int 作为 ViewType
        val type = clazz.name.hashCode()

        // 商业级防碰撞校验
        require(classToType[clazz] == null || classToType[clazz] == type) {
            "Fatal Error: ViewType hash collision for class: ${clazz.name}!"
        }

        classToType[clazz] = type
        binders.put(type, binder as ItemBinder<Any, ViewBinding>)
        return this
    }

    override fun getItemViewType(position: Int): Int {
        val clazz = getItem(position).javaClass
        return classToType[clazz] ?: throw IllegalArgumentException("Unregistered class: ${clazz.name}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder<ViewBinding> {
        val binder = binders[viewType] ?: throw IllegalStateException("No binder found for type: $viewType")
        val inflater = LayoutInflater.from(parent.context)
        // 直接利用传入的方法引用实例化 ViewBinding
        val binding = binder.inflate(inflater, parent, false)
        return BindingViewHolder(binding)
    }

    // 处理全量刷新
    override fun onBindViewHolder(holder: BindingViewHolder<ViewBinding>, position: Int) {
        val binder = binders[holder.itemViewType]
        binder(holder.binding, getItem(position))
    }

    // 处理局部刷新（Adapter 承担路由分发的 if-else）
    override fun onBindViewHolder(
        holder: BindingViewHolder<ViewBinding>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val binder = binders[holder.itemViewType]
        if (payloads.isEmpty()) {
            binder(holder.binding, getItem(position))
        } else {
            binder(holder.binding, getItem(position), payloads)
        }
    }

    // 提供给 MultiTypeDiffCallback 内部使用
    internal fun getBinder(clazz: Class<*>): ItemBinder<Any, ViewBinding>? {
        val type = classToType[clazz] ?: return null
        return binders[type]
    }

    class BindingViewHolder<B : ViewBinding>(val binding: B) : RecyclerView.ViewHolder(binding.root)

    internal class MultiTypeDiffCallback : DiffUtil.ItemCallback<Any>() {

        lateinit var adapter: MultiTypeAdapter

        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            if (oldItem.javaClass != newItem.javaClass) return false
            val binder = adapter.getBinder(oldItem.javaClass) ?: return false
            return binder.areItemsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            if (oldItem.javaClass != newItem.javaClass) return false
            val binder = adapter.getBinder(oldItem.javaClass) ?: return false
            return binder.areContentsTheSame(oldItem, newItem)
        }

        override fun getChangePayload(oldItem: Any, newItem: Any): Any? {
            if (oldItem.javaClass != newItem.javaClass) return null
            val binder = adapter.getBinder(oldItem.javaClass) ?: return null
            return binder.getChangePayload(oldItem, newItem)
        }
    }
}