package com.demo.jetpack.paging

import com.demo.jetpack.core.data.remote.Repo

data class RepoItem(
    val repo: Repo,
    val isSelected: Boolean = false
)
