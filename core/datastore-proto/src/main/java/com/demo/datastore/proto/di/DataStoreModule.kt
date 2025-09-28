package com.demo.datastore.proto.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.demo.core.common.AppDispatchers
import com.demo.core.common.ApplicationScope
import com.demo.core.common.Dispatcher
import com.demo.core.datastore.Task
import com.demo.datastore.proto.serializer.TaskSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

private const val TASK_DATA_STORE_FILE_NAME = "task.pb"

@InstallIn(SingletonComponent::class)
@Module
object DataStoreModule {

    @Provides
    @Singleton
    fun provideTaskPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Task> = DataStoreFactory.create(
        serializer = TaskSerializer,
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
    ) {
        context.dataStoreFile(TASK_DATA_STORE_FILE_NAME)
    }

}