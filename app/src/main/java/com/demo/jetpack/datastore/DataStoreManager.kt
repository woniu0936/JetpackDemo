package com.demo.jetpack.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.demo.core.datastore.Task
import com.demo.core.datastore.json.gson.Note
import com.demo.core.datastore.json.ktx.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES_FILE_NAME = "user_settings"

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userManagerDataStore: DataStore<User>,
    private val taskManagerDataStore: DataStore<Task>,
    private val noteManagerDataStore: DataStore<Note>
) {

    private val preferencesDataStore: DataStore<Preferences> by lazy {
        context.dataStore
    }

    private val stringKey = stringPreferencesKey("string_key")
    private val intKey = intPreferencesKey("int_key")
    private val longKey = longPreferencesKey("long_key")
    private val floatKey = floatPreferencesKey("float_key")
    private val doubleKey = doublePreferencesKey("double_key")
    private val booleanKey = booleanPreferencesKey("boolean_key")
    private val stringSetKey = stringSetPreferencesKey("string_set_key")

    suspend fun saveData(value: Any) {
        preferencesDataStore.edit {
            when (value) {
                is String -> it[stringKey] = value
                is Int -> it[intKey] = value
                is Long -> it[longKey] = value
                is Float -> it[floatKey] = value
                is Double -> it[doubleKey] = value
                is Boolean -> it[booleanKey] = value
                is Set<*> -> it[stringSetKey] = value as Set<String>
            }
        }
    }

    val stringFlow: Flow<String> = preferencesDataStore.data.map { it[stringKey] ?: "" }
    val intFlow: Flow<Int> = preferencesDataStore.data.map { it[intKey] ?: 0 }
    val longFlow: Flow<Long> = preferencesDataStore.data.map { it[longKey] ?: 0L }
    val floatFlow: Flow<Float> = preferencesDataStore.data.map { it[floatKey] ?: 0f }
    val doubleFlow: Flow<Double> = preferencesDataStore.data.map { it[doubleKey] ?: 0.0 }
    val booleanFlow: Flow<Boolean> = preferencesDataStore.data.map { it[booleanKey] ?: false }
    val stringSetFlow: Flow<Set<String>> = preferencesDataStore.data.map { it[stringSetKey] ?: emptySet() }

    fun readUser(): Flow<User> {
        return userManagerDataStore.data
    }

    suspend fun updateUser(user: User) {
        userManagerDataStore.updateData { currentUser ->
            val currentTime = System.currentTimeMillis()
            currentUser.copy(
                id = user.id,
                name = user.name,
                age = user.age,
                createTime = if (currentUser.createTime == 0L) currentTime else currentUser.createTime,
                modifyTime = currentTime
            )
        }
    }

    fun readTask(): Flow<Task> {
        return taskManagerDataStore.data
    }

    suspend fun updateTask(task: Task) {
        taskManagerDataStore.updateData { currentTask ->
            Log.d("DataStoreManager", "Current Task: ${currentTask.toFormattedString()}")
            val currentTime = System.currentTimeMillis()
            currentTask.toBuilder()
                .setId(task.id)
                .setTitle(task.title)
                .setContent(task.content)
                .setCreateTime(if (currentTask.createTime == 0L) currentTime else currentTask.createTime)
                .setModifyTime(currentTime)
                .build()
        }
    }

    fun readNote(): Flow<Note> {
        return noteManagerDataStore.data
    }

    suspend fun updateNote(note: Note) {
        noteManagerDataStore.updateData { currentNote ->
            val currentTime = System.currentTimeMillis()
            currentNote.copy(
                id = note.id,
                title = note.title,
                content = note.content,
                createTime = if (currentNote.createTime == 0L) currentTime else currentNote.createTime,
                modifyTime = currentTime
            )
        }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_FILE_NAME)

fun Task.toFormattedString(): String {
    return """
        Task ID: $id
        Task Title: $title
        Task Content: $content
        Create Time: $createTime
        Modify Time: $modifyTime
    """.trimIndent()
}