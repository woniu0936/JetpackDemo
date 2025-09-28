package com.demo.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.demo.core.common.AppDispatchers
import com.demo.core.common.ApplicationScope
import com.demo.core.common.Dispatcher
import com.demo.core.datastore.model.Note
import com.demo.core.datastore.model.User
import com.demo.core.datastore.serializer.NoteSerializer
import com.demo.core.datastore.serializer.UserSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

private const val USER_DATA_STORE_FILE_NAME = "user.pb"
private const val TASK_DATA_STORE_FILE_NAME = "task.pb"
private const val NOTE_DATA_STORE_FILE_NAME = "note.json"

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

    @Provides
    @Singleton
    fun provideNotePreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Note> = DataStoreFactory.create(
        serializer = NoteSerializer,
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
    ) {
        context.dataStoreFile(NOTE_DATA_STORE_FILE_NAME)
    }
}