package com.demo.jetpack.core.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.demo.jetpack.core.data.remote.Repo

@Dao
interface RepoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repos: List<Repo>)

    @Query("SELECT * FROM repos ORDER BY starCount DESC")
    fun getAllRepos(): PagingSource<Int, Repo>

    @Query("DELETE FROM repos WHERE id IN (:repoIds)")
    suspend fun deleteRepos(repoIds: Set<Int>)

    @Query("DELETE FROM repos")
    suspend fun clearRepos()

    @Query("SELECT COUNT(id) FROM repos")
    suspend fun getRepoCount(): Int
}
