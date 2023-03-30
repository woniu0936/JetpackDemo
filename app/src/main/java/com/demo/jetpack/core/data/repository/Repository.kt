package com.demo.jetpack.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.demo.jetpack.core.data.remote.GitHubService
import com.demo.jetpack.core.data.remote.Repo
import com.demo.jetpack.core.data.remote.RepoPagingSource
import kotlinx.coroutines.flow.Flow

object Repository {

    private const val PAGE_SIZE = 50

    private val gitHubService = GitHubService.create()

    fun getPagingData(): Flow<PagingData<Repo>> {
        return Pager(
            config = PagingConfig(PAGE_SIZE),
            pagingSourceFactory = { RepoPagingSource(gitHubService) }
        ).flow
    }

}