package com.demo.jetpack.adapter

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.demo.core.common.adapter.BaseAdapter
import com.demo.core.common.adapter.buildAdapter
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.ActivityBaseAdapterBinding
import com.demo.jetpack.databinding.RepoItemBinding

class BaseAdapterActivity : AppCompatActivity() {

    private val binding: ActivityBaseAdapterBinding by viewBinding(ActivityBaseAdapterBinding::inflate)
    private lateinit var adapter: BaseAdapter<RepoInfo, RepoItemBinding>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        adapter = buildAdapter<RepoInfo, RepoItemBinding>(RepoItemBinding::inflate) {
            onBind { item, _ ->
                nameText.text = item.name
                descriptionText.text = item.description
                starCountText.text = "★ ${item.stars}"
                checkbox.isChecked = item.selected
            }

            onBindPayloads { item, position, payloads ->
                when {
                    payloads.contains(PAYLOAD_SELECTION) -> checkbox.isChecked = item.selected
                    payloads.contains(PAYLOAD_STARS) -> starCountText.text = "★ ${item.stars}"
                    else -> {
                        nameText.text = item.name
                        descriptionText.text = item.description
                        starCountText.text = "★ ${item.stars}"
                        checkbox.isChecked = item.selected
                    }
                }
            }

            onClick { item, _ ->
                val next = adapter.getCurrentList().map { repo ->
                    if (repo.id == item.id) repo.copy(stars = repo.stars + 1, selected = !repo.selected)
                    else repo
                }
                adapter.setData(next)
                Toast.makeText(
                    this@BaseAdapterActivity,
                    "已为 ${item.name} 增加 1 颗星，选中状态已切换",
                    Toast.LENGTH_SHORT
                ).show()
            }

            onLongClick { item, _ ->
                Toast.makeText(
                    this@BaseAdapterActivity,
                    "长按：${item.name}\n当前星数：${item.stars}",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        adapter.setData(sampleRepos())
    }

    private fun sampleRepos() = listOf(
        RepoInfo(1, "Jetpack Core", "一套高质量的 Android 基础组件封装。", 88, false),
        RepoInfo(2, "Compose Demo", "展示 Compose 与 ViewBinding 混合方案。", 63, false),
        RepoInfo(3, "DataStore 工具", "轻量级键值与 Proto 数据存储封装。", 125, false),
        RepoInfo(4, "Crash 日志", "App 崩溃捕获与上报组件。", 47, false),
        RepoInfo(5, "RecyclerView 基类", "通用 BaseAdapter 的典型应用。", 31, false)
    )

    private data class RepoInfo(
        val id: Int,
        val name: String,
        val description: String,
        val stars: Int,
        val selected: Boolean
    )

    companion object {
        private const val PAYLOAD_STARS = "PAYLOAD_STARS"
        private const val PAYLOAD_SELECTION = "PAYLOAD_SELECTION"
    }
}
