# 无限循环列表库 - 快速上手指南

## 1. 简介

本指南将帮助你快速集成和使用高性能、低内存、API友好且极其稳定的无限循环列表功能。

## 2. 快速上手教程 (Quick Start)

只需简单四步，即可在你的 `ViewPager2` 中集成优雅的无限循环功能。

### 第1步：创建你的 Adapter

根据你的需求，选择继承 `InfiniteAdapter` (基础版) 或 `InfiniteListAdapter` (高性能版)。这里我们以高性能版为例。

首先，为你的数据类创建一个 `DiffUtil.ItemCallback`。

```kotlin
// YourData.kt
data class BannerItem(val id: Int, val title: String, val imageUrl: String)

// BannerDiffCallback.kt
class BannerDiffCallback : DiffUtil.ItemCallback<BannerItem>() {
    override fun areItemsTheSame(oldItem: BannerItem, newItem: BannerItem): Boolean {
        return oldItem.id == newItem.id // 使用唯一ID判断是否是同一个Item
    }
    override fun areContentsTheSame(oldItem: BannerItem, newItem: BannerItem): Boolean {
        return oldItem == newItem // 使用 data class 的 equals 判断内容是否一致
    }
}
```

然后，创建你的 `Adapter`，继承 `InfiniteListAdapter`，并实现 `onBindRealViewHolder` 方法来绑定UI。

```kotlin
// BannerAdapter.kt
class BannerAdapter : InfiniteListAdapter<BannerItem, ItemBannerBinding>(
    ItemBannerBinding::inflate, // 传入 ViewBinding 的 inflate 方法
    BannerDiffCallback()        // 传入 DiffUtil.ItemCallback 实例
) {
    // 在这里实现你的UI绑定逻辑，非常纯粹！
    override fun onBindRealViewHolder(binding: ItemBannerBinding, realPosition: Int, data: BannerItem) {
        binding.bannerTitle.text = data.title
        // a_image_loader.load(data.imageUrl).into(binding.bannerImage)
    }
}
```

### 第2步：在 Fragment / Activity 中设置

在你的 `Fragment` 或 `Activity` 中，初始化 `Adapter` 和 `ViewPager2`，然后调用我们的扩展函数进行设置。

```kotlin
// HomeFragment.kt
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var viewPager: ViewPager2
    private val bannerAdapter = BannerAdapter() // 创建你的 Adapter 实例

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewPager = view.findViewById(R.id.banner_view_pager)

        // 核心设置：只需调用这个扩展函数
        viewPager.setupInfiniteAdapter(bannerAdapter)
    }
}
```

### 第3步：加载并提交数据

当你的数据从网络或数据库加载完成后，提交给 `Adapter`，然后调用 `jumpToInfiniteStart()` 来启动无限循环。

```kotlin
// HomeFragment.kt (续)

    private fun loadData() {
        // 模拟从 ViewModel 加载数据
        val bannerList = listOf(
            BannerItem(1, "新品发布", "..."),
            BannerItem(2, "热门活动", "..."),
            BannerItem(3, "限时折扣", "...")
        )

        // 1. 提交数据列表
        bannerAdapter.submitList(bannerList)

        // 2. 【关键】跳转到无限循环的起始位置
        // 这个方法是安全的，内部会保证只在首次加载时执行一次
        viewPager.jumpToInfiniteStart()
    }
```

### 第4步：(可选) 添加监听器

你可以轻松地添加页面切换和点击事件的监听器。

```kotlin
// HomeFragment.kt -> onViewCreated (续)

    // 添加页面切换监听器 (回调的是真实位置)
    viewPager.onInfinitePageChange { realPosition ->
        // 在这里更新你的页面指示器 (Indicator)
        // indicator.selection = realPosition
        Log.d("HomeFragment", "当前页面真实位置: $realPosition")
    }

    // 添加 Item 点击监听器 (回调的是真实位置)
    bannerAdapter.onItemClickListener = { bannerItem, realPosition ->
        Toast.makeText(context, "点击了: ${bannerItem.title}", Toast.LENGTH_SHORT).show()
        
        // 你还可以用我们的扩展函数来控制滚动
        // 比如点击最后一项，让它滚动回第一项
        if (realPosition == bannerAdapter.getRealCount() - 1) {
            viewPager.setCurrentInfiniteItem(0, 300L) // 300ms 滚动到第0项
        }
    }
```

**恭喜！** 你已经成功实现了一个高性能、功能完备的无限循环 `ViewPager2`。

## 3. 核心概念详解

### 3.1 两种适配器模式

你可以根据场景选择最合适的适配器。

*   **`InfiniteAdapter` (简单适配器)**
    适用于数据一次性加载、很少变化的场景。它使用 `notifyDataSetChanged()` 进行全量刷新。

    **@example: 创建一个独立的 Adapter 类**
    ```kotlin
    class ImageBannerAdapter : InfiniteAdapter<String, ItemImageBannerBinding>(
        ItemImageBannerBinding::inflate
    ) {
        override fun ItemImageBannerBinding.onBindRealViewHolder(realPosition: Int, data: String) {
            Glide.with(this.root).load(data).into(this.bannerImageView)
        }
    }
    // 使用
    val adapter = ImageBannerAdapter()
    bannerView.setAdapter(adapter)
    adapter.submitList(imageUrls)
    ```

*   **`InfiniteListAdapter` (高性能适配器)**
    适用于数据动态变化（如来自网络请求、用户操作增删）的场景。它基于 `DiffUtil`，可以实现高效的局部刷新和动画。

    **@example: 创建一个 ListAdapter**
    ```kotlin
    // 数据类
    data class BannerItem(val id: String, val imageUrl: String)
    // DiffUtil.ItemCallback
    object BannerDiff : DiffUtil.ItemCallback<BannerItem>() {
        override fun areItemsTheSame(oldItem: BannerItem, newItem: BannerItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BannerItem, newItem: BannerItem) = oldItem == newItem
    }
    
    // 使用便捷扩展函数
    val listAdapter = bannerView.setupWithBannerListAdapter(BannerDiff, ItemImageBannerBinding::inflate) { _, data ->
        Glide.with(root).load(data.imageUrl).into(bannerImageView)
    }
    listAdapter.submitList(listOf(BannerItem("1", "url1")))
    ```

### 3.2 页面过渡动画 (PageTransformer)

为 Banner 切换添加炫酷的视觉效果。

*   **使用内置动画**

    **@example:**
    ```kotlin
    import com.yourcompany.banner.transformer.GalleryTransformer
    import com.yourcompany.banner.transformer.ScaleTransformer
    
    // 应用画廊效果
    bannerView.setPageTransformer(GalleryTransformer(minScale = 0.9f))
    
    // 或应用缩放效果
    bannerView.setPageTransformer(ScaleTransformer())
    ```
*   **自定义动画**
    只需实现 `BannerPageTransformer` 接口（即 `ViewPager2.PageTransformer`）即可。

### 3.3 核心方法解析

*   `fun ViewPager2.jumpToInfiniteStart()`
    *   **作用**: 在首次加载数据后，将 `ViewPager2` 跳转到一个巨大的中间位置，以实现左右都能无限滑动。
    *   **为什么需要它？** 如果从第 `0` 项开始，就无法向左滑动了。

*   `fun ViewPager2.setCurrentInfiniteItem(realPosition: Int, duration: Long)`
    *   **作用**: 以**恒定的速度**，平滑地将 `ViewPager2` 滚动到指定的**真实位置**。
    *   **代码解析**: 
        *   **最短路径计算**: 
            ```kotlin
            var diff = realPosition - currentRealItem
            if (abs(diff) > realCount / 2) { ... }
            ```
            这段代码智能地计算环形列表中的最短滚动距离。
        *   **速度控制**: 
            > **对初级开发者**: `duration` 参数的含义是 **“滚动1000个像素所需的时间”**。传入 `200L` 就能获得一个比默认更平滑的滚动动画。

*   `fun ViewPager2.setCurrentInfiniteRawItem(virtualPosition: Int, ...)`
    *   **作用**: 这是一个**高级API**，可以直接将 `ViewPager2` 滚动到指定的**虚拟位置**。

## 4. 总结

这份指南涵盖了无限循环列表库的快速集成和核心概念。希望能帮助你和你的团队轻松地在项目中集成并使用这个强大的组件。
