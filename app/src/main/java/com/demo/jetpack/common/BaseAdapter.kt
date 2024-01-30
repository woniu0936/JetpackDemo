package com.demo.jetpack.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.demo.jetpack.core.extension.isTrue

abstract class BaseAdapter<T, VB : ViewBinding> : RecyclerView.Adapter<BaseAdapter.BaseViewHolder<VB>>() {

    private var mList: List<T> = emptyList()

    private var mItemClickListener: ((View, T, Int) -> Unit)? = null
    private var mItemLongClickListener: ((View, T, Int) -> Boolean)? = null

    abstract fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup): VB

    abstract fun VB.onBindView(item: T, position: Int)

    open fun VB.onBindView(item: T, position: Int, payloads: MutableList<Any>) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = onCreateBinding(LayoutInflater.from(parent.context), parent)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        val item = getItem(position)
        holder.binding.onBindView(item, position)

        holder.binding.root.setOnClickListener {
            mItemClickListener?.invoke(it, item, position)
        }

        holder.binding.root.setOnLongClickListener {
            val result = mItemLongClickListener?.invoke(it, item, position)
            result.isTrue
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = getItem(position)
            holder.binding.onBindView(item, position, payloads)
        }
    }

    override fun getItemCount(): Int = mList.size

    fun setItemClickListener(listener: (View, T, Int) -> Unit) {
        mItemClickListener = listener
    }

    fun setItemLongClickListener(listener: (View, T, Int) -> Boolean) {
        mItemLongClickListener = listener
    }

    private fun getItem(position: Int): T = mList[position]

    fun currentList() = mList

    fun submitList(list: List<T>) {
        mList = list
        notifyDataSetChanged()
    }

    class BaseViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)
}