package com.demo.jetpack.core.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface GitHubService {

    @GET("search/repositories?sort=stars&q=Android")
    suspend fun searchRepos(@Query("page") page: Int, @Query("per_page") perPage: Int): RepoResponse

}