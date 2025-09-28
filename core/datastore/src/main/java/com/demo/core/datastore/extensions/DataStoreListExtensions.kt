package com.demo.core.datastore.extensions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.IOException

/**
 * [内部] 创建一个可配置的、容错的 Json 实例，专门用于 List 的序列化。
 */
private val safeJsonForList = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = false
}

/**
 * [内部] 辅助函数，用于根据元素的序列化器创建 List 的序列化器。
 */
private fun <E> createListSerializer(
    elementSerializer: KSerializer<E>
): KSerializer<List<E>> = ListSerializer(elementSerializer)


// =======================================================================================
// ===                             读取操作 (Read Operations)                            ===
// =======================================================================================

/**
 * 以 Flow 的形式，安全地、响应式地获取一个存储为 JSON 字符串的 List。
 *
 * 如果键不存在、JSON 损坏或发生 IO 错误，将返回提供的 [defaultValue]。
 */
fun <E> DataStore<Preferences>.getListFlow(
    keyName: String,
    elementSerializer: KSerializer<E>,
    defaultValue: List<E> = emptyList()
): Flow<List<E>> {
    val key = stringPreferencesKey(keyName)
    val listSerializer = createListSerializer(elementSerializer)

    return this.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonString = preferences[key]
            if (jsonString.isNullOrEmpty()) {
                defaultValue
            } else {
                try {
                    safeJsonForList.decodeFromString(listSerializer, jsonString)
                } catch (e: SerializationException) {
                    defaultValue
                }
            }
        }
}

/**
 * 一次性地、同步地获取存储为 JSON 字符串的 List。
 * ⚠️ **重要**: DataStore 操作是 main-safe 的，但对于可能耗时的操作，仍建议在后台 Dispatcher 中调用。
 */
suspend fun <E> DataStore<Preferences>.getList(
    keyName: String,
    elementSerializer: KSerializer<E>,
    defaultValue: List<E> = emptyList()
): List<E> {
    return getListFlow(keyName, elementSerializer, defaultValue).first()
}

/**
 * 一次性地、同步地获取 List 中的单个元素。
 * @return 返回指定索引的元素，如果索引越界或发生错误，则返回 `null`。
 */
suspend fun <E> DataStore<Preferences>.getFromList(
    keyName: String,
    elementSerializer: KSerializer<E>,
    index: Int
): E? {
    val list = getList(keyName, elementSerializer)
    return list.getOrNull(index)
}


// =======================================================================================
// ===                            写入与更新操作 (Write Operations)                        ===
// =======================================================================================

/**
 * 用一个新值【完全替换】存储在 Preferences 中的 List。
 */
suspend fun <E> DataStore<Preferences>.setList(
    keyName: String,
    elementSerializer: KSerializer<E>,
    value: List<E>
) {
    val key = stringPreferencesKey(keyName)
    val listSerializer = createListSerializer(elementSerializer)

    this.edit { preferences ->
        if (value.isEmpty()) {
            preferences.remove(key) // 空列表时直接移除键，节省空间
        } else {
            preferences[key] = safeJsonForList.encodeToString(listSerializer, value)
        }
    }
}

/**
 * 原子性地向 List 的【末尾】添加一个新元素。
 */
suspend fun <E> DataStore<Preferences>.addToList(
    keyName: String,
    elementSerializer: KSerializer<E>,
    element: E
) {
    val key = stringPreferencesKey(keyName)
    val listSerializer = createListSerializer(elementSerializer)

    this.edit { preferences ->
        val currentJson = preferences[key]
        val currentList = if (currentJson.isNullOrEmpty()) {
            mutableListOf()
        } else {
            try {
                safeJsonForList.decodeFromString(listSerializer, currentJson).toMutableList()
            } catch (e: SerializationException) {
                mutableListOf()
            }
        }

        currentList.add(element)
        preferences[key] = safeJsonForList.encodeToString(listSerializer, currentList)
    }
}

/**
 * 原子性地从 List 中移除【第一个匹配】的元素。
 */
suspend fun <E> DataStore<Preferences>.removeFromList(
    keyName: String,
    elementSerializer: KSerializer<E>,
    element: E
) {
    val key = stringPreferencesKey(keyName)
    val listSerializer = createListSerializer(elementSerializer)

    this.edit { preferences ->
        val currentJson = preferences[key]
        if (currentJson.isNullOrEmpty()) return@edit

        val currentList = try {
            safeJsonForList.decodeFromString(listSerializer, currentJson).toMutableList()
        } catch (e: SerializationException) {
            return@edit
        }

        currentList.remove(element)

        if (currentList.isEmpty()) {
            preferences.remove(key)
        } else {
            preferences[key] = safeJsonForList.encodeToString(listSerializer, currentList)
        }
    }
}

/**
 * 清空整个 List，通过移除其顶级键来实现。
 */
suspend fun DataStore<Preferences>.clearList(
    keyName: String
) {
    val key = stringPreferencesKey(keyName)
    this.edit { preferences ->
        preferences.remove(key)
    }
}

/**
 * 原子性地对整个 List 进行任意的批量更新。
 * 这是执行多个复杂操作（增、删、改、排序、过滤等）的最高效、最安全的方式。
 */
suspend fun <E> DataStore<Preferences>.updateList(
    keyName: String,
    elementSerializer: KSerializer<E>,
    transform: (currentList: List<E>) -> List<E>
) {
    val key = stringPreferencesKey(keyName)
    val listSerializer = createListSerializer(elementSerializer)

    this.edit { preferences ->
        val currentJson = preferences[key]
        val currentList = if (currentJson.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                safeJsonForList.decodeFromString(listSerializer, currentJson)
            } catch (e: SerializationException) {
                emptyList()
            }
        }

        val newList = transform(currentList)

        if (newList.isEmpty()) {
            preferences.remove(key)
        } else {
            preferences[key] = safeJsonForList.encodeToString(listSerializer, newList)
        }
    }
}


// =======================================================================================
// ===                    “最后一公里”的便捷扩展 (User-Facing API)                     ===
// =======================================================================================

/* -------- List<String> -------- */

/**
 * [便捷API] 获取一个 `List<String>` 的响应式 Flow。
 */
fun DataStore<Preferences>.getStringListFlow(
    keyName: String,
    defaultValue: List<String> = emptyList()
): Flow<List<String>> {
    return getListFlow(keyName, String.serializer(), defaultValue)
}

/**
 * [便捷API] 原子性地向 `List<String>` 的末尾添加一个元素。
 */
suspend fun DataStore<Preferences>.addToStringList(
    keyName: String,
    element: String
) {
    addToList(keyName, String.serializer(), element)
}

/* -------- List<Int> -------- */

/**
 * [便捷API] 获取一个 `List<Int>` 的响应式 Flow。
 */
fun DataStore<Preferences>.getIntListFlow(
    keyName: String,
    defaultValue: List<Int> = emptyList()
): Flow<List<Int>> {
    return getListFlow(keyName, Int.serializer(), defaultValue)
}

/**
 * [便捷API] 原子性地向 `List<Int>` 的末尾添加一个元素。
 */
suspend fun DataStore<Preferences>.addToIntList(
    keyName: String,
    element: Int
) {
    addToList(keyName, Int.serializer(), element)
}

/* -------- 适用于任意可序列化对象的通用 API (进阶) -------- */

/**
 * [进阶] 以 Flow 的形式，响应式地获取一个存储了【任意可序列化对象】的 List。
 * @param E 列表中元素的类型，必须被 `@Serializable` 注解标记。
 */
inline fun <reified E : Any> DataStore<Preferences>.getSerializableListFlow(
    keyName: String,
    defaultValue: List<E> = emptyList()
): Flow<List<E>> {
    return getListFlow(keyName, serializer(), defaultValue)
}

/**
 * [进阶] 原子性地向存储了【任意可序列化对象】的 List 的末尾添加一个元素。
 */
suspend inline fun <reified E : Any> DataStore<Preferences>.addToSerializableList(
    keyName: String,
    element: E
) {
    addToList(keyName, serializer(), element)
}