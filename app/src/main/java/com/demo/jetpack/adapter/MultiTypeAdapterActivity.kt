package com.demo.jetpack.adapter

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.demo.core.common.adapter.MultiTypeAdapter
import com.demo.core.common.adapter.register
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.ActivityBaseListAdapterBinding
import com.demo.jetpack.databinding.ItemHeaderBinding
import com.demo.jetpack.databinding.ItemPromotionBinding
import com.demo.jetpack.databinding.RepoItemBinding

/**
 * MultiTypeAdapter 的完整使用示例。
 *
 * 演示核心特性：
 * 1. 单个 RecyclerView 中混合展示多种完全不同的数据类型
 * 2. 每种类型有独立的布局、Binding、Diff、Payload、事件逻辑
 * 3. 自动路由：根据数据类型匹配对应的 Binder
 * 4. 编译期类型安全 + 运行时自动分发
 *
 * 数据模型：
 * - TextHeaderItem: 分类头部
 * - RepoItem: 仓库条目
 * - PromotionItem: 推广条目
 */
class MultiTypeAdapterActivity : AppCompatActivity() {

    private val binding: ActivityBaseListAdapterBinding by viewBinding(ActivityBaseListAdapterBinding::inflate)

    private val adapter = MultiTypeAdapter()
        // ==========================================
        // 注册类型 1: TextHeaderItem (蓝色标题头)
        // ==========================================
        .register<TextHeaderItem, ItemHeaderBinding>(ItemHeaderBinding::inflate) {
            itemDiffId { item -> item.id }
            areContentsTheSame { old, new -> old == new }

            onBindView { item ->
                headerTitle.text = item.title
                headerSubtitle.text = item.subtitle
            }
        }

        // ==========================================
        // 注册类型 2: RepoItem (标准仓库条目)
        // ==========================================
        .register<RepoItem, RepoItemBinding>(RepoItemBinding::inflate) {
            itemDiffId { item -> item.id }
            areContentsTheSame { old, new -> old == new }
            getChangePayload { old, new ->
                when {
                    old.stars != new.stars && old.selected != new.selected -> PAYLOAD_REPO_BOTH
                    old.stars != new.stars -> PAYLOAD_REPO_STARS
                    old.selected != new.selected -> PAYLOAD_REPO_SELECTION
                    else -> null
                }
            }

            onBindView { item ->
                nameText.text = item.name
                descriptionText.text = item.description
                starCountText.text = "★ ${item.stars}"
                checkbox.isChecked = item.selected
            }

            onBindViewPayloads { item, payloads ->
                when {
                    payloads.contains(PAYLOAD_REPO_BOTH) -> {
                        starCountText.text = "★ ${item.stars}"
                        checkbox.isChecked = item.selected
                    }

                    payloads.contains(PAYLOAD_REPO_STARS) -> {
                        starCountText.text = "★ ${item.stars}"
                    }

                    payloads.contains(PAYLOAD_REPO_SELECTION) -> {
                        checkbox.isChecked = item.selected
                    }

                    else -> {
                        nameText.text = item.name
                        descriptionText.text = item.description
                        starCountText.text = "★ ${item.stars}"
                        checkbox.isChecked = item.selected
                    }
                }
            }

            onClick { position, item ->
                updateListItem(position, item.copy(
                    stars = item.stars + 1,
                    selected = !item.selected
                ))
                Toast.makeText(
                    this@MultiTypeAdapterActivity,
                    "已为 ${item.name} 增加 1 颗星",
                    Toast.LENGTH_SHORT
                ).show()
            }

            onLongClick { _, item ->
                Toast.makeText(
                    this@MultiTypeAdapterActivity,
                    "长按：${item.name}\n当前星数：${item.stars}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // ==========================================
        // 注册类型 3: PromotionItem (橙色推广条)
        // ==========================================
        .register<PromotionItem, ItemPromotionBinding>(ItemPromotionBinding::inflate) {
            itemDiffId { item -> item.id }
            areContentsTheSame { old, new -> old == new }

            onBindView { item ->
                promoTitle.text = item.title
                promoDesc.text = item.description
                promoButton.text = item.buttonText
            }

            onClick { _, item ->
                Toast.makeText(
                    this@MultiTypeAdapterActivity,
                    "点击推广：${item.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        // 构建混合列表，展示多类型处理能力
        val mixedList = listOf(
            TextHeaderItem(id = "h1", title = "🚀 推荐库", subtitle = "最新发布的高质量库"),
            RepoItem(1, "Jetpack Core", "一套高质量的 Android 基础组件封装。", 88, false),
            PromotionItem(id = "p1", title = "现在订阅", description = "享受专属优惠和更新", buttonText = "立即订阅"),
            RepoItem(2, "Compose Demo", "展示 Compose 与 ViewBinding 混合方案。", 63, false),
            TextHeaderItem(id = "h2", title = "📦 工具库", subtitle = "生产级数据存储与日志"),
            RepoItem(3, "DataStore 工具", "轻量级键值与 Proto 数据存储封装。", 125, false),
            RepoItem(4, "Crash 日志", "App 崩溃捕获与上报组件。", 47, false),
            PromotionItem(id = "p2", title = "升级到 Pro", description = "获得企业级功能支持", buttonText = "了解详情"),
            RepoItem(5, "RecyclerView 基类", "通用 MultiTypeAdapter 的典型应用。", 31, false)
        ) as List<Any>

        adapter.submitList(mixedList)
    }

    /**
     * 辅助函数：更新列表中的某个 item
     */
    private fun updateListItem(position: Int, newItem: Any) {
        val currentList = adapter.currentList.toMutableList()
        if (position in currentList.indices) {
            currentList[position] = newItem
            adapter.submitList(currentList.toList())
        }
    }

    // ==========================================
    // 数据模型定义
    // ==========================================

    /**
     * 类型 1: 文本标题头
     */
    private data class TextHeaderItem(
        val id: String,
        val title: String,
        val subtitle: String
    )

    /**
     * 类型 2: 仓库信息
     */
    private data class RepoItem(
        val id: Int,
        val name: String,
        val description: String,
        val stars: Int,
        val selected: Boolean
    )

    /**
     * 类型 3: 推广条目
     */
    private data class PromotionItem(
        val id: String,
        val title: String,
        val description: String,
        val buttonText: String
    )

    companion object {
        // RepoItem Payload 标识符
        private const val PAYLOAD_REPO_STARS = "PAYLOAD_REPO_STARS"
        private const val PAYLOAD_REPO_SELECTION = "PAYLOAD_REPO_SELECTION"
        private const val PAYLOAD_REPO_BOTH = "PAYLOAD_REPO_BOTH"
    }
}
