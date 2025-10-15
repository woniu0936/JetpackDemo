
---

## **BannerView 库 - 快速上手指南**

### **1. 简介**

`BannerView` 是一个功能强大、高度可定制且生命周期安全的商业级 Banner 组件。它旨在以最简洁的 API 提供最稳定、最丰富的 Banner 功能。

**核心特性:**

*   **无限循环**：基于 UI 处理的无缝循环方案，专为 Banner 场景优化。
*   **自动播放**：支持自定义间隔时间的自动轮播，并能在用户触摸时智能暂停。
*   **生命周期安全**：可与 `Activity/Fragment` 的生命周期绑定，自动管理播放和停止，杜绝内存泄漏。
*   **两种适配器模式**：
    *   `BannerAdapter`：简单易用，适用于静态或不常变化的数据。
    *   `BannerListAdapter`：基于 `DiffUtil`，为动态数据提供高性能的局部刷新和优雅的动画效果。
*   **可插拔的指示器**：内置圆形指示器，并提供标准 `Indicator` 接口供无限扩展。
*   **可插拔的过渡动画**：内置画廊、缩放等多种 `PageTransformer`，并允许自定义。
*   **流畅的 API 设计**：支持链式调用和 Kotlin 作用域函数，提供极致的开发体验。

### **2. 快速集成**

#### **第一步：添加源码**

将本库的 `com/yourcompany/banner/` 目录下的所有源码文件复制到你的项目中。

#### **第二步：添加资源文件**

在你的主模块（或库模块）的 `res/values/` 目录下，确保有以下两个文件：

**`res/values/attrs.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="BannerView">
        <attr name="banner_loopTime" format="integer" />
        <attr name="banner_isAutoLoop" format="boolean" />
        <attr name="banner_scrollTime" format="integer" />
        <attr name="banner_offscreenPageLimit" format="integer" />
        <attr name="banner_transformer" format="enum">
            <enum name="gallery" value="0" />
            <enum name="scale" value="1" />
        </attr>
    </declare-styleable>
</resources>
```

**`res/values/ids.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <item name="banner_internal_callback_tag" type="id" />
</resources>
```

### **3. 基础用法**

这是最快让 Banner 跑起来的方式。

#### **第一步：在 XML 布局中添加 BannerView**

```xml
<com.yourcompany.banner.BannerView
    android:id="@+id/bannerView"
    android:layout_width="match_parent"
    android:layout_height="200dp"
    app:banner_isAutoLoop="true"
    app:banner_loopTime="3000" />
```

#### **第二步：创建适配器并绑定数据**

假设你的数据是一个 `String` 列表（图片 URL）。

1.  **创建你的 Item 布局** (`item_image_banner.xml`):
    ```xml
    <ImageView xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@+id/bannerImageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="centerCrop" />
    ```

2.  **在你的 `Activity` 或 `Fragment` 中进行设置**:
    ```kotlin
    // 引入你的 ViewBinding 类
    import com.yourproject.databinding.ItemImageBannerBinding
    
    class HomeFragment : Fragment() {
        
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            
            val bannerView = view.findViewById<BannerView>(R.id.bannerView)
            val imageUrls = listOf("url1.jpg", "url2.jpg", "url3.jpg")
            
            // 使用便捷的 setupWith... 扩展函数，无需创建 Adapter 类文件
            val adapter = bannerView.setupWithBannerAdapter(ItemImageBannerBinding::inflate) { _, data ->
                // 'this' 是 ItemImageBannerBinding，可以直接访问视图
                // 使用 Glide 或其他图片库加载图片
                Glide.with(this.root).load(data).into(this.bannerImageView)
            }
            
            // 提交数据
            adapter.submitList(imageUrls)
            
            // (强烈推荐) 绑定生命周期
            bannerView.attachToLifecycle(viewLifecycleOwner)
        }
    }
    ```

### **4. 核心概念详解**

#### **4.1 两种适配器模式**

你可以根据场景选择最合适的适配器。

*   **`BannerAdapter` (简单适配器)**
    适用于数据一次性加载、很少变化的场景。它使用 `notifyDataSetChanged()` 进行全量刷新。

    **@example: 创建一个独立的 Adapter 类**
    ```kotlin
    class ImageBannerAdapter : BannerAdapter<String, ItemImageBannerBinding>(
        ItemImageBannerBinding::inflate
    ) {
        override fun ItemImageBannerBinding.onBannerBind(realPosition: Int, data: String) {
            Glide.with(root).load(data).into(bannerImageView)
        }
    }
    // 使用
    val adapter = ImageBannerAdapter()
    bannerView.setAdapter(adapter)
    adapter.submitList(imageUrls)
    ```

*   **`BannerListAdapter` (高性能适配器)**
    适用于数据动态变化（如来自网络请求、用户操作增删）的场景。它基于 `DiffUtil`，可以实现高效的局部刷新和动画。

    **@example: 创建一个 ListAdapter**
    ```kotlin
    // 数据类
    data class BannerItem(val id: String, val imageUrl: String)
    // DiffUtil.ItemCallback
    object BannerDiff : DiffUtil.ItemCallback<BannerItem>() {
        override fun areItemsTheSame(old: BannerItem, new: BannerItem) = old.id == new.id
        override fun areContentsTheSame(old: BannerItem, new: BannerItem) = old == new
    }
    
    // 使用便捷扩展函数
    val listAdapter = bannerView.setupWithBannerListAdapter(BannerDiff, ItemImageBannerBinding::inflate) { _, data ->
        Glide.with(root).load(data.imageUrl).into(bannerImageView)
    }
    listAdapter.submitList(listOf(BannerItem("1", "url1")))
    ```

#### **4.2 指示器 (Indicator)**

`BannerView` 支持添加任意自定义指示器。

*   **使用内置圆形指示器**

    **@example:**
    ```kotlin
    import com.yourcompany.banner.indicator.CircleIndicator
    
    val indicator = CircleIndicator(requireContext()).apply {
        // (可选) 自定义颜色
        setIndicatorColor(Color.parseColor("#88FFFFFF"))
        setSelectedIndicatorColor(Color.parseColor("#FFFFFFFF"))
    }
    bannerView.setIndicator(indicator, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
    ```
*   **自定义指示器**
    只需实现 `Indicator` 接口，并重写 `getIndicatorView()` 和 `onPageChanged()` 方法即可。

#### **4.3 页面过渡动画 (PageTransformer)**

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

### **5. 高级 API 和技巧**

#### **5.1 链式调用与 `configure`**

你可以将多个配置操作链接在一起，或使用 `configure` 作用域函数聚合它们，让代码更优雅。

**@example:**
```kotlin
bannerView.setAdapter(myAdapter)
    .configure {
        isAutoLoopEnabled = true
        autoLoopIntervalMillis = 4000
        setIndicator(CircleIndicator(context))
        setPageTransformer(ScaleTransformer())
    }
    .setOnItemClickListener { data, position -> /* ... */ }
    .attachToLifecycle(viewLifecycleOwner)
```

#### **5.2 点击事件**

为 Banner 的 Item 添加点击事件。

**@example:**
```kotlin
bannerView.setOnItemClickListener { data, realPosition ->
    val bannerItem = data as BannerItem // 转型为你自己的数据类
    // 执行跳转、Toast 或其他操作
    Toast.makeText(context, "点击了第 $realPosition 项, ID: ${bannerItem.id}", Toast.LENGTH_SHORT).show()
}
```

#### **5.3 直接提交数据**

如果你不想保留 `Adapter` 的引用，可以直接在 `BannerView` 上提交数据。

**@example:**
```kotlin
bannerView.setAdapter(myAdapter)
// ...
viewModel.bannerData.observe(viewLifecycleOwner) { newData ->
    bannerView.submitData(newData)
}
```

### **6. XML 属性参考**

你可以在 XML 布局文件中直接配置以下属性：

| 属性名 | 格式 | 描述 | 默认值 |
| --- | --- | --- | --- |
| `app:banner_isAutoLoop` | `boolean` | 是否自动循环播放 | `true` |
| `app:banner_loopTime` | `integer` | 自动轮播的间隔时间 (毫秒) | `3000` |
| `app:banner_offscreenPageLimit`| `integer` | 离屏预加载的页面数量 | `1` |
| `app:banner_transformer` | `enum` | 内置的过渡动画 | `none` |
| | `gallery` | 画廊效果 | |
| | `scale` | 缩放效果 | |

---

这份文档涵盖了从入门到精通 `BannerView` 库的全部内容。希望能帮助你和你的团队轻松地在项目中集成并使用这个强大的组件。