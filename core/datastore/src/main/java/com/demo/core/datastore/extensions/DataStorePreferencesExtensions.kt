package com.demo.core.datastore.extensions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// =======================================================================================
// ===                          响应式 Flow API (推荐用于 UI)                          ===
// =======================================================================================

/**
 * 内部核心函数，为 DataStore 的【响应式读取】操作提供统一的错误处理封装。
 *
 * @param key 要读取的数据对应的 Key。
 * @param defaultValue 当键不存在或发生 IO 异常时返回的默认值。
 * @return 一个持续发射最新值的 `Flow`。
 */
private fun <T> DataStore<Preferences>.getValueFlow(
    key: Preferences.Key<T>,
    defaultValue: T
): Flow<T> = this.data
    .catch { exception ->
        // [关键] 当 DataStore 读取文件失败时（如文件损坏），会抛出 IOException。
        // 我们捕获这个异常，并将其转换为发射一个空的 Preferences 对象。
        if (exception is IOException) {
            // 这里可以添加日志上报，用于监控线上的数据损坏情况
            // Log.e("DataStore", "Error reading preferences for key: ${key.name}", exception)
            emit(emptyPreferences())
        } else {
            // 对于其他非 IO 异常（如 CancellationException），继续向上抛出。
            throw exception
        }
    }
    .map { preferences ->
        // 从 Preferences 对象中获取值。如果键不存在，`?:` 操作符会返回我们提供的 defaultValue。
        preferences[key] ?: defaultValue
    }

/* -------- Flow-based 读取操作 (Getters) -------- */

/**
 * 以 Flow 的形式，响应式地、安全地从 DataStore 中获取一个 String 值。
 * 这是与 UI 绑定的首选方式，当值发生变化时，UI 会自动更新。
 */
fun DataStore<Preferences>.getStringFlow(key: Preferences.Key<String>, defaultValue: String = ""): Flow<String> =
    getValueFlow(key, defaultValue)

/**
 * 以 Flow 的形式，响应式地、安全地从 DataStore 中获取一个 Int 值。
 */
fun DataStore<Preferences>.getIntFlow(key: Preferences.Key<Int>, defaultValue: Int = 0): Flow<Int> =
    getValueFlow(key, defaultValue)

/**
 * 以 Flow 的形式，响应式地、安全地从 DataStore 中获取一个 Boolean 值。
 */
fun DataStore<Preferences>.getBooleanFlow(key: Preferences.Key<Boolean>, defaultValue: Boolean = false): Flow<Boolean> =
    getValueFlow(key, defaultValue)

/**
 * 以 Flow 的形式，响应式地、安全地从 DataStore 中获取一个 Long 值。
 */
fun DataStore<Preferences>.getLongFlow(key: Preferences.Key<Long>, defaultValue: Long = 0L): Flow<Long> =
    getValueFlow(key, defaultValue)

/**
 * 以 Flow 的形式，响应式地、安全地从 DataStore 中获取一个 Float 值。
 */
fun DataStore<Preferences>.getFloatFlow(key: Preferences.Key<Float>, defaultValue: Float = 0f): Flow<Float> =
    getValueFlow(key, defaultValue)

/**
 * 以 Flow 的形式，响应式地、安全地从 DataStore 中获取一个 Double 值。
 */
fun DataStore<Preferences>.getDoubleFlow(key: Preferences.Key<Double>, defaultValue: Double = 0.0): Flow<Double> =
    getValueFlow(key, defaultValue)

/**
 * 以 Flow 的形式，响应式地、安全地从 DataStore 中获取一个 Set<String> 值。
 */
fun DataStore<Preferences>.getStringSetFlow(key: Preferences.Key<Set<String>>, defaultValue: Set<String> = emptySet()): Flow<Set<String>> =
    getValueFlow(key, defaultValue)


// =======================================================================================
// ===                      一次性同步 API (推荐用于后台任务)                        ===
// =======================================================================================

/**
 * 内部核心函数，为 DataStore 的【一次性同步读取】操作提供统一的错误处理和线程安全。
 *
 * @return 返回获取到的值；或在发生任何异常时，安全地返回 defaultValue。
 */
private suspend fun <T> DataStore<Preferences>.getValueSyncInternal(
    key: Preferences.Key<T>,
    defaultValue: T
): T {
    return try {
        val preferences = this.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .first()

        preferences[key] ?: defaultValue
    } catch (exception: Exception) {
        // [兜底] 这是一个最终的保护层，以防 first() 或其他操作抛出未知异常。
        // Log.e("DataStore", "Failed to get value synchronously for key: ${key.name}", exception)
        defaultValue
    }
}

/* -------- 同步读取操作 (Synchronous Getters) -------- */

/**
 * 同步地、安全地从 DataStore 中获取一个 String 值。
 * ⚠️ **重要**: 此函数【必须】在后台线程（如 `Dispatchers.IO`）中调用。
 */
suspend fun DataStore<Preferences>.getValueSync(key: Preferences.Key<String>, defaultValue: String = ""): String =
    getValueSyncInternal(key, defaultValue)

/**
 * 同步地、安全地从 DataStore 中获取一个 Int 值。
 * ⚠️ **重要**: 此函数【必须】在后台线程（如 `Dispatchers.IO`）中调用。
 */
suspend fun DataStore<Preferences>.getValueSync(key: Preferences.Key<Int>, defaultValue: Int = 0): Int =
    getValueSyncInternal(key, defaultValue)

/**
 * 同步地、安全地从 DataStore 中获取一个 Boolean 值。
 * ⚠️ **重要**: 此函数【必须】在后台线程（如 `Dispatchers.IO`）中调用。
 */
suspend fun DataStore<Preferences>.getValueSync(key: Preferences.Key<Boolean>, defaultValue: Boolean = false): Boolean =
    getValueSyncInternal(key, defaultValue)

/**
 * 同步地、安全地从 DataStore 中获取一个 Long 值。
 * ⚠️ **重要**: 此函数【必须】在后台线程（如 `Dispatchers.IO`）中调用。
 */
suspend fun DataStore<Preferences>.getValueSync(key: Preferences.Key<Long>, defaultValue: Long = 0L): Long =
    getValueSyncInternal(key, defaultValue)

/**
 * 同步地、安全地从 DataStore 中获取一个 Float 值。
 * ⚠️ **重要**: 此函数【必须】在后台线程（如 `Dispatchers.IO`）中调用。
 */
suspend fun DataStore<Preferences>.getValueSync(key: Preferences.Key<Float>, defaultValue: Float = 0f): Float =
    getValueSyncInternal(key, defaultValue)

/**
 * 同步地、安全地从 DataStore 中获取一个 Double 值。
 * ⚠️ **重要**: 此函数【必须】在后台线程（如 `Dispatchers.IO`）中调用。
 */
suspend fun DataStore<Preferences>.getValueSync(key: Preferences.Key<Double>, defaultValue: Double = 0.0): Double =
    getValueSyncInternal(key, defaultValue)

/**
 * 同步地、安全地从 DataStore 中获取一个 Set<String> 值。
 * ⚠️ **重要**: 此函数【必须】在后台线程（如 `Dispatchers.IO`）中调用。
 */
suspend fun DataStore<Preferences>.getValueSync(key: Preferences.Key<Set<String>>, defaultValue: Set<String> = emptySet()): Set<String> =
    getValueSyncInternal(key, defaultValue)


// =======================================================================================
// ===                          通用的写入与删除 API                               ===
// =======================================================================================

/**
 * 一个通用的、高性能的设值函数。
 *
 * 使用 `inline` 关键字可以避免函数调用的额外开销，并将 `edit` lambda 的内容内联到调用处。
 *
 * ---
 * ### 使用示例:
 * ```
 * viewModelScope.launch {
 *     dataStore.setValue(PrefKeys.USER_NAME, "Alice")
 *     dataStore.setValue(PrefKeys.LAUNCH_COUNT, 10)
 * }
 * ```
 */
suspend inline fun <T> DataStore<Preferences>.setValue(key: Preferences.Key<T>, value: T) {
    this.edit { preferences ->
        preferences[key] = value
    }
}

/**
 * 一个通用的、类型安全的移除函数。
 *
 * ---
 * ### 使用示例:
 * ```
 * viewModelScope.launch {
 *     dataStore.remove(PrefKeys.AUTH_TOKEN)
 * }
 * ```
 */
suspend inline fun <T> DataStore<Preferences>.remove(key: Preferences.Key<T>) {
    this.edit { preferences ->
        preferences.remove(key)
    }
}