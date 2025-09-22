package com.demo.jetpack.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preferences DataStore a complete example
 *
 * Preferences DataStore 使用键来访问存储的值，它不像 Proto DataStore 那样需要预定义的架构。
 * 虽然这可能看起来更简单，但它缺乏 Proto DataStore 提供的类型安全性。
 *
 * 主要优点:
 * 1. 简单性: 无需定义 .proto 文件，可以直接使用键值对。
 * 2. 快速集成: 对于简单数据（如用户设置），集成速度更快。
 *
 * 主要缺点:
 * 1. 类型不安全: 编译器无法在运行时捕获类型错误（例如，你尝试将一个 Int 读作 String）。
 * 2. 缺乏架构: 没有强制性的数据结构，可能导致数据不一致。
 *
 * 适用场景:
 * - 存储非常简单的、扁平化的用户偏好设置，例如主题（暗/亮模式）、通知开关等。
 * - 当你不需要复杂的数据结构，并且愿意牺牲一些类型安全性以换取更快的开发速度时。
 */

// 1. 使用 `by preferencesDataStore` 委托在 Context 的顶层创建一个 DataStore 实例。
//    `name` 参数是此 DataStore 实例的唯一标识符。
private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    private val stringKey = stringPreferencesKey("string_key")
    private val intKey = intPreferencesKey("int_key")
    private val longKey = longPreferencesKey("long_key")
    private val floatKey = floatPreferencesKey("float_key")
    private val doubleKey = doublePreferencesKey("double_key")
    private val booleanKey = booleanPreferencesKey("boolean_key")
    private val stringSetKey = stringSetPreferencesKey("string_set_key")

    suspend fun saveData(value: Any) {
        context.preferencesDataStore.edit {
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

    val stringFlow: Flow<String> = context.preferencesDataStore.data.map { it[stringKey] ?: "" }
    val intFlow: Flow<Int> = context.preferencesDataStore.data.map { it[intKey] ?: 0 }
    val longFlow: Flow<Long> = context.preferencesDataStore.data.map { it[longKey] ?: 0L }
    val floatFlow: Flow<Float> = context.preferencesDataStore.data.map { it[floatKey] ?: 0f }
    val doubleFlow: Flow<Double> = context.preferencesDataStore.data.map { it[doubleKey] ?: 0.0 }
    val booleanFlow: Flow<Boolean> = context.preferencesDataStore.data.map { it[booleanKey] ?: false }
    val stringSetFlow: Flow<Set<String>> = context.preferencesDataStore.data.map { it[stringSetKey] ?: emptySet() }
}