package com.demo.jetpack.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFERENCES_FILE_NAME = "user_settings"
private const val USER_DATA_STORE_FILE_NAME = "user.pb"

class DataStoreManager(private val context: Context) {

    private val preferencesDataStore: DataStore<Preferences> by lazy {
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        context.dataStore
    }

    private val userManagerDataStore: DataStore<User> by lazy {
        DataStoreFactory.create(
            serializer = UserSerializer,
            produceFile = { context.filesDir.resolve(USER_DATA_STORE_FILE_NAME) }
        )
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
        userManagerDataStore.updateData {
            user
        }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_FILE_NAME)
