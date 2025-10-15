package com.demo.core.view


import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 一个持有 ViewBinding 实例的通用 ViewHolder。
 *
 * @param VB ViewBinding 类的类型。
 * @property binding 项目布局的 ViewBinding 实例。
 */
class BindingViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)