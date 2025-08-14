package com.demo.jetpack.core.data.module

import android.content.Context
import com.demo.jetpack.core.data.local.RepoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): RepoDatabase {
        return RepoDatabase.getInstance(context)
    }

    @Provides
    fun provideRepoDao(database: RepoDatabase) = database.repoDao()

    @Provides
    fun provideRemoteKeysDao(database: RepoDatabase) = database.remoteKeysDao()
}
