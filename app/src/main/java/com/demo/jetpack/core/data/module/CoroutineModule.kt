package com.demo.jetpack.core.data.module

import com.demo.jetpack.hilt.AppDispatchers
import com.demo.jetpack.hilt.ApplicationScope
import com.demo.jetpack.hilt.Dispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    @Dispatcher(AppDispatchers.IO)
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @Dispatcher(AppDispatchers.Default)
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @Dispatcher(AppDispatchers.Main)
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @Dispatcher(AppDispatchers.Default) defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
}
