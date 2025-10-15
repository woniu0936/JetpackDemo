@file:JvmName("BannerViewExtensions")

package com.demo.core.view.banner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.demo.core.view.banner.adapter.BannerAdapter
import com.demo.core.view.banner.adapter.BannerListAdapter

/**
 * 为 [BannerView] 提供一组高质量的扩展函数，旨在提升开发者体验，
 * 实现类型安全的配置和流畅的链式调用。
 */

/**
 * 使用一个配置块来初始化或修改 BannerView 的属性。
 * 这利用了 Kotlin 的作用域函数，将所有相关配置聚合在一起，提高了代码的可读性。
 *
 * @param block 一个在 [BannerView] 上下文中执行的 lambda 表达式。
 * @return [BannerView] 实例本身，以支持进一步的链式调用。
 *
 * @example
 * ```kotlin
 * bannerView.configure {
 *     isAutoLoopEnabled = true
 *     autoLoopIntervalMillis = 4000
 *     offscreenPageLimit = 2
 *     setIndicator(CircleIndicator(context))
 *     setPageTransformer(GalleryTransformer())
 * }.attachToLifecycle(viewLifecycleOwner) // 可以在配置块外部继续链式调用
 * ```
 */
inline fun BannerView.configure(block: BannerView.() -> Unit): BannerView {
    this.apply(block)
    return this
}

/**
 * 为 BannerView 设置一个基于 [BannerAdapter] 的适配器，并返回该适配器实例以供后续操作。
 * 这是一个类型安全的构建器，它简化了适配器的创建和设置过程。
 *
 * @param T 数据项的类型。
 * @param VB 视图绑定的类型。
 * @param bindingInflater [ViewBinding] 类的 `inflate` 方法引用。
 * @param onBindBlock 数据绑定的核心逻辑。这是一个在 `VB` 上下文中执行的扩展函数 lambda。
 * @return 创建好的 [BannerAdapter] 实例。
 *
 * @example
 * ```kotlin
 * // 无需再单独创建一个 Adapter 类文件！
 * val simpleAdapter = bannerView.setupWithBannerAdapter(ItemBannerImageBinding::inflate) { realPosition, data ->
 *     // 'this' 是 ItemBannerImageBinding
 *     Glide.with(root).load(data).into(bannerImageView)
 * }
 * simpleAdapter.submitList(listOf("url1", "url2"))
 * ```
 */
inline fun <T, VB : ViewBinding> BannerView.setupWithBannerAdapter(
    noinline bindingInflater: (inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean) -> VB,
    crossinline onBindBlock: VB.(realPosition: Int, data: T) -> Unit
): BannerAdapter<T, VB> {
    val bannerAdapter = object : BannerAdapter<T, VB>(bindingInflater) {
        override fun VB.onBannerBind(realPosition: Int, data: T) {
            onBindBlock(this, realPosition, data)
        }
    }
    this.setAdapter(bannerAdapter)
    return bannerAdapter
}

/**
 * 为 BannerView 设置一个基于 [BannerListAdapter] 的高性能适配器，并返回该适配器实例。
 * 这是一个类型安全的构建器，封装了 `DiffUtil.ItemCallback` 和适配器的创建。
 *
 * @param T 数据项的类型，必须拥有稳定的 ID。
 * @param VB 视图绑定的类型。
 * @param diffCallback [DiffUtil.ItemCallback] 的实现。
 * @param bindingInflater [ViewBinding] 类的 `inflate` 方法引用。
 * @param onBindBlock 数据绑定的核心逻辑。
 * @return 创建好的 [BannerListAdapter] 实例。
 *
 * @example
 * ```kotlin
 * // 同样无需创建 Adapter 类文件
 * val listAdapter = bannerView.setupWithBannerListAdapter(
 *     diffCallback = BannerDiffCallback, // 你的 DiffUtil.ItemCallback
 *     bindingInflater = ItemBannerImageBinding::inflate
 * ) { realPosition, data ->
 *     // 'this' 是 ItemBannerImageBinding
 *     Glide.with(root).load(data.imageUrl).into(bannerImageView)
 * }
 * listAdapter.submitList(listOf(BannerItem("1", "url1")))
 * ```
 */
inline fun <T, VB : ViewBinding> BannerView.setupWithBannerListAdapter(
    diffCallback: DiffUtil.ItemCallback<T>,
    noinline bindingInflater: (inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean) -> VB,
    crossinline onBindBlock: VB.(realPosition: Int, data: T) -> Unit
): BannerListAdapter<T, VB> {
    val bannerListAdapter = object : BannerListAdapter<T, VB>(diffCallback, bindingInflater) {
        override fun VB.onBannerBind(realPosition: Int, data: T) {
            onBindBlock(this, realPosition, data)
        }
    }
    this.setAdapter(bannerListAdapter)
    return bannerListAdapter
}

/**
 * 设置当 Banner 的 item 被点击时的回调。
 * 这是一个比直接设置 `onItemClickListener` 属性更具 Kotlin 风格的函数式 API。
 *
 * @param listener 一个 lambda 表达式，当 item 被点击时会携带数据和真实位置被调用。
 * @return [BannerView] 实例，以支持链式调用。
 *
 * @example
 * ```kotlin
 * bannerView.setOnItemClickListener { data, position ->
 *     val bannerItem = data as BannerItem
 *     // 处理点击事件
 * }.attachToLifecycle(viewLifecycleOwner)
 * ```
 */
fun BannerView.setOnItemClickListener(listener: (data: Any, realPosition: Int) -> Unit): BannerView {
    this.onItemClickListener = listener
    return this
}