package com.demo.jetpack.core.data.module

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.demo.jetpack.datastore.Note
import com.demo.jetpack.datastore.NoteSerializer
import com.demo.jetpack.datastore.Task
import com.demo.jetpack.datastore.TaskSerializer
import com.demo.jetpack.datastore.User
import com.demo.jetpack.datastore.UserSerializer
import com.demo.jetpack.hilt.AppDispatchers
import com.demo.jetpack.hilt.ApplicationScope
import com.demo.jetpack.hilt.Dispatcher
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
private const val PREFERENCES_DATA_STORE_FILE_NAME = "user_settings"

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