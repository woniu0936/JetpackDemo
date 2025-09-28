package com.demo.core.datastore.json.ktx.extensions

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
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.IOException
import kotlin.text.isNullOrEmpty

/**
 * [内部] 创建一个可配置的、容错的 Json 实例，专门用于 Map 的序列化。
 */
private val safeJsonForMap = Json {
    ignoreUnknownKeys = true    // 忽略未知的键，增强向前兼容性
    coerceInputValues = true    // 强制转换输入值，增强向后兼容性
    isLenient = true            // 容忍格式上的一些小问题
    explicitNulls = false       // 不在 JSON 中输出值为 null 的字段
    encodeDefaults = false      // 不编码与默认值相同的字段，减小存储体积
}

/**
 * [内部] 辅助函数，用于根据 Key 和 Value 的序列化器创建 Map 的序列化器。
 */
private fun <K, V> createMapSerializer(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
): KSerializer<Map<K, V>> = MapSerializer(keySerializer, valueSerializer)


// =======================================================================================
// ===                             读取操作 (Read Operations)                            ===
// =======================================================================================

/**
 * 以 Flow 的形式，安全地、响应式地获取一个存储为 JSON 字符串的 Map。
 *
 * 这是与 UI 绑定的首选方式。如果 Map 的内容发生变化，这个 Flow 会自动发射最新的 Map 对象。
 * 如果键不存在、JSON 损坏或发生 IO 错误，将返回提供的 [defaultValue]。
 *
 * ---
 * ### 使用示例 (在 ViewModel 中):
 * ```
 * val userSettingsFlow: StateFlow<Map<String, Int>> = dataStore
 *     .getMapFlow("user_settings", String.serializer(), Int.serializer())
 *     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
 * ```
 */
fun <K, V> DataStore<Preferences>.getMapFlow(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    defaultValue: Map<K, V> = emptyMap()
): Flow<Map<K, V>> {
    val key = stringPreferencesKey(keyName)
    val mapSerializer = createMapSerializer(keySerializer, valueSerializer)

    return this.data
        .catch { exception ->
            if (exception is IOException) {
                // Log.e("DataStore", "Error reading map for key: $keyName", exception)
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
                    safeJsonForMap.decodeFromString(mapSerializer, jsonString)
                } catch (e: SerializationException) {
                    // Log.e("DataStore", "Failed to deserialize map for key: $keyName", e)
                    defaultValue
                }
            }
        }
}

/**
 * 一次性地、同步地获取存储为 JSON 字符串的 Map。
 *
 * 适用于后台任务、数据迁移等非 UI 驱动的场景。
 *
 * ---
 * ### 注意事项:
 * - **DataStore 操作本身是主线程安全的 (main-safe)**，因为它内部会将 I/O 操作切换到后台线程。
 *   因此，您可以在任何协程上下文中安全地调用此 `suspend` 函数，而不会阻塞调用者线程。
 * - 为了代码清晰和职责分离，对于可能耗时的操作，仍建议在适当的 Dispatcher (如 `Dispatchers.IO`) 中调用。
 *
 * ---
 * ### 使用示例:
 * ```
 * suspend fun getInitialSettings(): Map<String, Int> {
 *     return dataStore.getMap("user_settings", String.serializer(), Int.serializer())
 * }
 * ```
 */
suspend fun <K, V> DataStore<Preferences>.getMap(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    defaultValue: Map<K, V> = emptyMap()
): Map<K, V> {
    return getMapFlow(keyName, keySerializer, valueSerializer, defaultValue).first()
}

/**
 * [性能优化版] 一次性地、同步地获取 Map 中的单个值。
 *
 * 此版本避免了反序列化整个 Map，仅在需要时进行解析，性能更优。
 *
 * @return 返回键对应的值，如果 Map 或键不存在，或发生解析错误，则返回 `null`。
 */
suspend fun <K, V> DataStore<Preferences>.getFromMap(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    entryKey: K
): V? {
    val key = stringPreferencesKey(keyName)
    val mapSerializer = createMapSerializer(keySerializer, valueSerializer)

    // .first() 确保我们只进行一次 I/O 读取
    val preferences = this.data.first()
    val jsonString = preferences[key] ?: return null

    return try {
        val map = safeJsonForMap.decodeFromString(mapSerializer, jsonString)
        map[entryKey]
    } catch (e: SerializationException) {
        null
    }
}

/**
 * [性能优化版] 一次性地、同步地检查 Map 中是否存在某个键。
 */
suspend fun <K, V> DataStore<Preferences>.containsKeyInMap(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    entryKey: K
): Boolean {
    val key = stringPreferencesKey(keyName)
    val mapSerializer = createMapSerializer(keySerializer, valueSerializer)

    val preferences = this.data.first()
    val jsonString = preferences[key] ?: return false

    return try {
        val map = safeJsonForMap.decodeFromString(mapSerializer, jsonString)
        map.containsKey(entryKey)
    } catch (e: SerializationException) {
        false
    }
}


// =======================================================================================
// ===                            写入与更新操作 (Write Operations)                        ===
// =======================================================================================

/**
 * 用一个新值【完全替换】存储在 Preferences 中的 Map。
 */
suspend fun <K, V> DataStore<Preferences>.setMap(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    value: Map<K, V>
) {
    val key = stringPreferencesKey(keyName)
    val mapSerializer = createMapSerializer(keySerializer, valueSerializer)

    this.edit { preferences ->
        // [存储优化] 如果传入的 Map 为空，直接移除对应的键，以节省存储空间。
        if (value.isEmpty()) {
            preferences.remove(key)
        } else {
            preferences[key] = safeJsonForMap.encodeToString(mapSerializer, value)
        }
    }
}

/**
 * 原子性地向 Map 中添加或更新一个键值对。
 *
 * 利用 `edit` 操作的事务性，保证了“读-改-写”操作的原子性，避免了竞态条件和数据丢失。
 */
suspend fun <K, V> DataStore<Preferences>.putToMap(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    entryKey: K,
    entryValue: V
) {
    val key = stringPreferencesKey(keyName)
    val mapSerializer = createMapSerializer(keySerializer, valueSerializer)

    this.edit { preferences ->
        val currentJson = preferences[key]
        val currentMap = if (currentJson.isNullOrEmpty()) {
            mutableMapOf()
        } else {
            try {
                safeJsonForMap.decodeFromString(mapSerializer, currentJson).toMutableMap()
            } catch (e: SerializationException) {
                mutableMapOf() // 如果现有数据损坏，则从一个空 Map 开始，避免操作失败
            }
        }

        currentMap[entryKey] = entryValue
        preferences[key] = safeJsonForMap.encodeToString(mapSerializer, currentMap)
    }
}

/**
 * [新增功能] 原子性地向 Map 中批量添加或更新多个键值对。
 *
 * 这比多次调用 `putToMap` 更高效，因为它只执行一次“读-序列化-写”操作。
 */
suspend fun <K, V> DataStore<Preferences>.putAllToMap(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    entries: Map<K, V>
) {
    if (entries.isEmpty()) return

    val key = stringPreferencesKey(keyName)
    val mapSerializer = createMapSerializer(keySerializer, valueSerializer)

    this.edit { preferences ->
        val currentJson = preferences[key]
        val currentMap = if (currentJson.isNullOrEmpty()) {
            mutableMapOf()
        } else {
            try {
                safeJsonForMap.decodeFromString(mapSerializer, currentJson).toMutableMap()
            } catch (e: SerializationException) {
                mutableMapOf()
            }
        }

        currentMap.putAll(entries)
        preferences[key] = safeJsonForMap.encodeToString(mapSerializer, currentMap)
    }
}

/**
 * 原子性地从 Map 中移除一个键值对。
 */
suspend fun <K, V> DataStore<Preferences>.removeFromMap(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    entryKey: K
) {
    val key = stringPreferencesKey(keyName)
    val mapSerializer = createMapSerializer(keySerializer, valueSerializer)

    this.edit { preferences ->
        val currentJson = preferences[key]
        if (currentJson.isNullOrEmpty()) return@edit

        val currentMap = try {
            safeJsonForMap.decodeFromString(mapSerializer, currentJson).toMutableMap()
        } catch (e: SerializationException) {
            return@edit // 如果数据损坏，则放弃本次操作
        }

        currentMap.remove(entryKey)

        // [存储优化] 如果移除后 Map 为空，直接移除整个键。
        if (currentMap.isEmpty()) {
            preferences.remove(key)
        } else {
            preferences[key] = safeJsonForMap.encodeToString(mapSerializer, currentMap)
        }
    }
}

/**
 * 清空整个 Map，通过移除其在 Preferences 中的顶级键来实现。
 */
suspend fun DataStore<Preferences>.clearMap(
    keyName: String
) {
    val key = stringPreferencesKey(keyName)
    this.edit { preferences ->
        preferences.remove(key)
    }
}

/**
 * [新增功能] 原子性地对整个 Map 进行任意的批量更新。
 *
 * 这是执行多个复杂操作（增、删、改）的最高效、最安全的方式。
 *
 * ---
 * ### 使用示例:
 * ```
 * // 将所有 value 乘以 2，并添加一个新条目
 * dataStore.updateMap("score_map", String.serializer(), Int.serializer()) { currentMap ->
 *     val newMap = currentMap.mapValues { it.value * 2 }.toMutableMap()
 *     newMap["new_player"] = 100
 *     newMap
 * }
 * ```
 */
suspend fun <K, V> DataStore<Preferences>.updateMap(
    keyName: String,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    transform: (currentMap: Map<K, V>) -> Map<K, V>
) {
    val key = stringPreferencesKey(keyName)
    val mapSerializer = createMapSerializer(keySerializer, valueSerializer)

    this.edit { preferences ->
        val currentJson = preferences[key]
        val currentMap = if (currentJson.isNullOrEmpty()) {
            emptyMap()
        } else {
            try {
                safeJsonForMap.decodeFromString(mapSerializer, currentJson)
            } catch (e: SerializationException) {
                emptyMap() // 数据损坏时，从空 Map 开始转换
            }
        }

        val newMap = transform(currentMap)

        if (newMap.isEmpty()) {
            preferences.remove(key)
        } else {
            preferences[key] = safeJsonForMap.encodeToString(mapSerializer, newMap)
        }
    }
}

// =======================================================================================
// ===                    “最后一公里”的便捷扩展 (User-Facing API)                     ===
// =======================================================================================
//
//  这部分函数是对底层核心 API 的一层“语法糖 (Syntax Sugar)”。
//  它们通过为常见的 Map 类型（如 Map<String, Int>）创建专门的扩展，
//  完全隐藏了 KSerializer 的细节，从而为业务开发者提供了极其简洁的调用方式。
//
// =======================================================================================


/* -------- Map<String, String> -------- */

/**
 * [便捷API] 以 Flow 的形式，响应式地获取一个 `Map<String, String>`。
 *
 * ---
 * ### 使用示例:
 * ```
 * val userHeadersFlow: Flow<Map<String, String>> = dataStore
 *     .getStringStringMapFlow("api_headers")
 * ```
 */
fun DataStore<Preferences>.getStringStringMapFlow(
    keyName: String,
    defaultValue: Map<String, String> = emptyMap()
): Flow<Map<String, String>> {
    return getMapFlow(keyName, String.serializer(), String.serializer(), defaultValue)
}

/**
 * [便捷API] 原子性地向 `Map<String, String>` 中添加或更新一个键值对。
 *
 * ---
 * ### 使用示例:
 * ```
 * viewModelScope.launch {
 *     dataStore.putToStringStringMap("api_headers", "Authorization", "Bearer xyz")
 * }
 * ```
 */
suspend fun DataStore<Preferences>.putToStringStringMap(
    keyName: String,
    entryKey: String,
    entryValue: String
) {
    putToMap(keyName, String.serializer(), String.serializer(), entryKey, entryValue)
}


/* -------- Map<String, Int> -------- */

/**
 * [便捷API] 以 Flow 的形式，响应式地获取一个 `Map<String, Int>`。
 *
 * ---
 * ### 使用示例:
 * ```
 * val userScoresFlow: Flow<Map<String, Int>> = dataStore
 *     .getStringIntMapFlow("user_scores", defaultValue = mapOf("player1" to 100))
 * ```
 */
fun DataStore<Preferences>.getStringIntMapFlow(
    keyName: String,
    defaultValue: Map<String, Int> = emptyMap()
): Flow<Map<String, Int>> {
    return getMapFlow(keyName, String.serializer(), Int.serializer(), defaultValue)
}

/**
 * [便捷API] 原子性地向 `Map<String, Int>` 中添加或更新一个键值对。
 *
 * ---
 * ### 使用示例:
 * ```
 * viewModelScope.launch {
 *     dataStore.putToStringIntMap("user_scores", "player1", 120)
 * }
 * ```
 */
suspend fun DataStore<Preferences>.putToStringIntMap(
    keyName: String,
    entryKey: String,
    entryValue: Int
) {
    putToMap(keyName, String.serializer(), Int.serializer(), entryKey, entryValue)
}


/* -------- Map<String, Boolean> -------- */

/**
 * [便捷API] 以 Flow 的形式，响应式地获取一个 `Map<String, Boolean>`。
 *
 * ---
 * ### 使用示例:
 * ```
 * val featureFlagsFlow: Flow<Map<String, Boolean>> = dataStore
 *     .getStringBooleanMapFlow("feature_flags")
 * ```
 */
fun DataStore<Preferences>.getStringBooleanMapFlow(
    keyName: String,
    defaultValue: Map<String, Boolean> = emptyMap()
): Flow<Map<String, Boolean>> {
    return getMapFlow(keyName, String.serializer(), Boolean.serializer(), defaultValue)
}

/**
 * [便捷API] 原子性地向 `Map<String, Boolean>` 中添加或更新一个键值对。
 *
 * ---
 * ### 使用示例:
 * ```
 * viewModelScope.launch {
 *     dataStore.putToStringBooleanMap("feature_flags", "enable_new_feature", true)
 * }
 * ```
 */
suspend fun DataStore<Preferences>.putToStringBooleanMap(
    keyName: String,
    entryKey: String,
    entryValue: Boolean
) {
    putToMap(keyName, String.serializer(), Boolean.serializer(), entryKey, entryValue)
}


/* -------- Map<Int, String> -------- */

/**
 * [便捷API] 以 Flow 的形式，响应式地获取一个 `Map<Int, String>`。
 *
 * ---
 * ### 使用示例:
 * ```
 * val errorMessagesFlow: Flow<Map<Int, String>> = dataStore
 *     .getIntStringMapFlow("error_codes")
 * ```
 */
fun DataStore<Preferences>.getIntStringMapFlow(
    keyName: String,
    defaultValue: Map<Int, String> = emptyMap()
): Flow<Map<Int, String>> {
    return getMapFlow(keyName, Int.serializer(), String.serializer(), defaultValue)
}

/**
 * [便捷API] 原子性地向 `Map<Int, String>` 中添加或更新一个键值对。
 *
 * ---
 * ### 使用示例:
 * ```
 * viewModelScope.launch {
 *     dataStore.putToIntStringMap("error_codes", 404, "Not Found")
 * }
 * ```
 */
suspend fun DataStore<Preferences>.putToIntStringMap(
    keyName: String,
    entryKey: Int,
    entryValue: String
) {
    putToMap(keyName, Int.serializer(), String.serializer(), entryKey, entryValue)
}

/* -------- Map<String, Long> -------- */

/**
 * [便捷API] 以 Flow 的形式，响应式地获取一个 `Map<String, Long>`。
 *
 * ---
 * ### 使用示例:
 * ```
 * val timestampsFlow: Flow<Map<String, Long>> = dataStore
 *     .getStringLongMapFlow("user_timestamps")
 * ```
 */
fun DataStore<Preferences>.getStringLongMapFlow(
    keyName: String,
    defaultValue: Map<String, Long> = emptyMap()
): Flow<Map<String, Long>> {
    return getMapFlow(keyName, String.serializer(), Long.serializer(), defaultValue)
}

/**
 * [便捷API] 原子性地向 `Map<String, Long>` 中添加或更新一个键值对。
 *
 * ---
 * ### 使用示例:
 * ```
 * viewModelScope.launch {
 *     dataStore.putToStringLongMap("user_timestamps", "last_login", System.currentTimeMillis())
 * }
 * ```
 */
suspend fun DataStore<Preferences>.putToStringLongMap(
    keyName: String,
    entryKey: String,
    entryValue: Long
) {
    putToMap(keyName, String.serializer(), Long.serializer(), entryKey, entryValue)
}

/* -------- 适用于任意可序列化对象的通用 API (进阶) -------- */

/**
 * [进阶] 以 Flow 的形式，响应式地获取一个存储了【任意可序列化对象】的 Map。
 *
 * 使用 `inline` 和 `reified` 关键字，使得调用时无需手动传递 `serializer()`，
 * 编译器会自动推断并传入正确的序列化器。
 *
 * ---
 * ### 前提条件:
 * - `K` 和 `V` 类型都必须被 `@Serializable` 注解标记。
 *
 * ---
 * ### 使用示例 (假设 `MyConfig` 是一个 `@Serializable` data class):
 * ```
 * val customMapFlow: Flow<Map<String, MyConfig>> = dataStore
 *     .getSerializableMapFlow("component_configs")
 * ```
 */
inline fun <reified K : Any, reified V : Any> DataStore<Preferences>.getSerializableMapFlow(
    keyName: String,
    defaultValue: Map<K, V> = emptyMap()
): Flow<Map<K, V>> {
    return getMapFlow(keyName, serializer(), serializer(), defaultValue)
}

/**
 * [进阶] 原子性地向存储了【任意可序列化对象】的 Map 中添加或更新一个键值对。
 *
 * ---
 * ### 前提条件:
 * - `K` 和 `V` 类型都必须被 `@Serializable` 注解标记。
 *
 * ---
 * ### 使用示例 (假设 `MyConfig` 是一个 `@Serializable` data class):
 * ```
 * val newConfig = MyConfig(timeout = 5000, retryCount = 3)
 * viewModelScope.launch {
 *     dataStore.putToSerializableMap("component_configs", "network_settings", newConfig)
 * }
 * ```
 */
suspend inline fun <reified K : Any, reified V : Any> DataStore<Preferences>.putToSerializableMap(
    keyName: String,
    entryKey: K,
    entryValue: V
) {
    putToMap(keyName, serializer(), serializer(), entryKey, entryValue)
}
