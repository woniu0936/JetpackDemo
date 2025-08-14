package com.demo.jetpack.paging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.demo.jetpack.R
import javax.inject.Inject

class PagingAdapter @Inject constructor() : PagingDataAdapter<RepoItem, PagingAdapter.ViewHolder>(COMPARATOR) {

    var onItemClick: ((RepoItem) -> Unit)? = null

    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<RepoItem>() {
            override fun areItemsTheSame(oldItem: RepoItem, newItem: RepoItem): Boolean {
                return oldItem.repo.id == newItem.repo.id
            }

            override fun areContentsTheSame(oldItem: RepoItem, newItem: RepoItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_item, parent, false)
        val holder = ViewHolder(view)
        holder.itemView.setOnClickListener {
            getItem(holder.bindingAdapterPosition)?.let { repoItem ->
                onItemClick?.invoke(repoItem)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repoItem = getItem(position)
        if (repoItem != null) {
            holder.name.text = repoItem.repo.name
            holder.description.text = repoItem.repo.description
            holder.starCount.text = repoItem.repo.starCount.toString()
            holder.checkbox.isChecked = repoItem.isSelected
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name_text)
        val description: TextView = itemView.findViewById(R.id.description_text)
        val starCount: TextView = itemView.findViewById(R.id.star_count_text)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
    }

}