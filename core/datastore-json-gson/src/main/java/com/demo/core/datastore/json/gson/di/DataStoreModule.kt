package com.demo.core.datastore.json.gson.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.demo.core.common.annotation.AppDispatchers
import com.demo.core.common.annotation.ApplicationScope
import com.demo.core.common.annotation.Dispatcher
import com.demo.core.datastore.json.gson.Note
import com.demo.core.datastore.json.gson.serializer.NoteSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

private const val NOTE_DATA_STORE_FILE_NAME = "note.json"

@InstallIn(SingletonComponent::class)
@Module
object DataStoreModule {

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