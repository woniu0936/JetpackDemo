package com.demo.core.datastore.json.ktx.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.demo.core.common.AppDispatchers
import com.demo.core.common.ApplicationScope
import com.demo.core.common.Dispatcher
import com.demo.core.datastore.json.ktx.User
import com.demo.core.datastore.json.ktx.serializer.UserSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

private const val USER_DATA_STORE_FILE_NAME = "user.pb"

@InstallIn(SingletonComponent::class)
@Module
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<User> = DataStoreFactory.create(
        serializer = UserSerializer,
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
    ) {
        context.dataStoreFile(USER_DATA_STORE_FILE_NAME)
    }
}