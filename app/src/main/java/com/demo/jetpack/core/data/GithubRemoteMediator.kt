package com.demo.jetpack.core.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.demo.jetpack.core.data.local.RepoDatabase
import com.demo.jetpack.core.data.local.entity.RemoteKeys
import com.demo.jetpack.core.data.remote.GitHubService
import com.demo.jetpack.core.data.remote.Repo
import com.demo.jetpack.core.util.NetworkMonitor
import retrofit2.HttpException
import java.io.IOException
import com.demo.core.logger.logger // Add this import

private const val GITHUB_STARTING_PAGE_INDEX = 1

@OptIn(ExperimentalPagingApi::class)
class GithubRemoteMediator(
    private val service: GitHubService,
    private val database: RepoDatabase,
    private val networkMonitor: NetworkMonitor
) : RemoteMediator<Int, Repo>() {

    private val log by lazy { logger() } // Add logger instance

    override suspend fun initialize(): InitializeAction {
        log.d { "Initializing GithubRemoteMediator." }
        return if (networkMonitor.isConnected()) {
            log.d { "Network is connected, launching initial refresh." }
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            log.d { "Network is not connected, skipping initial refresh." }
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>): MediatorResult {
        log.d { "Loading data for loadType: $loadType" }
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                val nextPage = remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
                log.d { "LoadType.REFRESH, next page: $nextPage" }
                nextPage
            }
            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKeys?.prevKey ?: run {
                    log.d { "LoadType.PREPEND, no previous key found, end of pagination reached." }
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                log.d { "LoadType.PREPEND, previous key: $prevKey" }
                prevKey
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey ?: run {
                    log.d { "LoadType.APPEND, no next key found, end of pagination reached." }
                    return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                }
                log.d { "LoadType.APPEND, next key: $nextKey" }
                nextKey
            }
        }

        try {
            log.d { "Fetching repos from service for page $page with page size ${state.config.pageSize}" }
            val apiResponse = service.searchRepos(page, state.config.pageSize)
            val repos = apiResponse.items
            val endOfPaginationReached = repos.isEmpty()
            log.d { "API response received, items count: ${repos.size}, endOfPaginationReached: $endOfPaginationReached" }

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    log.d { "LoadType.REFRESH, clearing remote keys and repos." }
                    database.remoteKeysDao().clearRemoteKeys()
                    database.repoDao().clearRepos()
                }
                val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = repos.map {
                    RemoteKeys(repoId = it.id, prevKey = prevKey, nextKey = nextKey)
                }
                log.d { "Inserting ${keys.size} remote keys and ${repos.size} repos into database." }
                database.remoteKeysDao().insertAll(keys)
                database.repoDao().insertAll(repos)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            log.e(exception) { "IOException during remote mediator load." }
            return MediatorResult.Success(endOfPaginationReached = true)
        } catch (exception: HttpException) {
            log.e(exception) { "HttpException during remote mediator load." }
            return MediatorResult.Error(exception)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Repo>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { repo ->
                log.d { "Getting remote key for last item: ${repo.id}" }
                database.remoteKeysDao().remoteKeysRepoId(repo.id)
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, Repo>): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { repo ->
                log.d { "Getting remote key for first item: ${repo.id}" }
                database.remoteKeysDao().remoteKeysRepoId(repo.id)
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, Repo>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { repoId ->
                log.d { "Getting remote key closest to current position: $position for repoId: $repoId" }
                database.remoteKeysDao().remoteKeysRepoId(repoId)
            }
        }
    }
}