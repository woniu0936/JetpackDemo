package com.demo.jetpack.core.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.demo.jetpack.core.data.GithubRemoteMediator
import com.demo.jetpack.core.data.local.RepoDatabase
import com.demo.jetpack.core.data.remote.GitHubService
import com.demo.jetpack.core.data.remote.Repo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    private val gitHubService: GitHubService,
    private val database: RepoDatabase
) {

    companion object {
        private const val PAGE_SIZE = 50
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getPagingData(): Flow<PagingData<Repo>> {
        val pagingSourceFactory = { database.repoDao().getAllRepos() }
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            remoteMediator = GithubRemoteMediator(
                gitHubService,
                database
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }

    suspend fun deleteRepos(repoIds: Set<Int>) {
        database.repoDao().deleteRepos(repoIds)
    }
}
