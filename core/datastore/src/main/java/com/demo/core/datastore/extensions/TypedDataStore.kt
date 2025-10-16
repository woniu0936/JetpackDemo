package com.demo.core.datastore.extensions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.demo.core.common.annotation.AppDispatchers
import com.demo.core.common.annotation.ApplicationScope
import com.demo.core.common.annotation.Dispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 一个用于将对象序列化到文件的 Serializer 所必须遵守的契约。
 *
 * 它继承了 DataStore 的标准 Serializer，并额外要求实现者
 * 提供一个明确的文件后缀名，以确保不同类型的数据能够存储在不同的文件中，
 * 避免了文件名冲突和数据类型混淆。
 *
 * @param T The type of the data to be serialized.
 */
interface FileSerializer<T> : Serializer<T> {
    /**
     * 该序列化器对应的数据文件的后缀名（包括点号）。
     *
     * ---
     * ### 实现示例:
     * ```
     * // 对于 JSON
     * override val suffix: String = ".json"
     *
     * // 对于 Protobuf
     * override val suffix: String = ".pb"
     * ```
     */
    val suffix: String
}

/**
 * 一个高级别的、类型安全的 DataStore 管理器，用于存储和管理可序列化的数据对象。
 *
 * 这个类是 DataStore 复杂性的一个“门面 (Facade)”，为业务层提供了极其简洁、健壮
 * 且类型安全的 API，同时在底层使用了正确、高效的 DataStore<T> 实例。
 * 它通过 Hilt 注入为单例，确保在整个应用中只有一个实例。
 *
 * @param context ApplicationContext，由 Hilt 自动注入。
 */
@Singleton
class TypedDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * [核心机制] 使用 ConcurrentHashMap 缓存 Lazy<DataStore<*>>。
     * - `ConcurrentHashMap` 保证了线程安全的访问。
     * - `Lazy` 保证了每个 DataStore 实例的创建过程只会被执行一次，即使在多线程并发请求下。
     * 这种双重保障机制是实现高效、健壮的单例工厂的最佳实践。
     */
    private val dataStoreCache = ConcurrentHashMap<String, Lazy<DataStore<*>>>()

    /**
     * 内部函数，用于获取或创建一个给定 key 和 serializer 的 DataStore<T> 实例。
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> getOrCreateDataStore(key: String, serializer: FileSerializer<T>): DataStore<T> {
        val fileName = key + serializer.suffix

        return dataStoreCache.computeIfAbsent(key) {
            lazy {
                // 这是在函数内部创建 DataStore 的官方推荐方式。
                DataStoreFactory.create(
                    serializer = serializer,
                    scope = CoroutineScope(scope.coroutineContext + ioDispatcher), // 提供一个专门的协程作用域
                    produceFile = { context.dataStoreFile(fileName) }
                )
            }
        }.value as DataStore<T>
    }


    /* ======================================================================================= */
    /* ===                              公共 API (Public API)                                === */
    /* ======================================================================================= */

    /**
     * 以 Flow 的形式，响应式地获取一个可序列化对象。
     *
     * 这是与 UI 绑定的首选方式。当数据文件发生变化时，这个 Flow 会自动发射最新的对象。
     * 如果文件不存在、为空或读取/解析失败，它会优雅地回退并发射 Serializer 中定义的 `defaultValue`。
     *
     * ---
     * ### 注意事项:
     * - 这个 Flow 是长生命周期的，它在被收集 (collect) 期间会持续监听文件变化。
     * - 在 ViewModel 中使用时，推荐与 `stateIn` 操作符结合，将其转换为 `StateFlow`。
     *
     * ---
     * ### 使用示例 (在 ViewModel 中):
     * ```
     * val userFlow: StateFlow<User> = dataStoreManager
     *     .getObjectFlow(DataStoreKeys.USER_PROFILE, UserSerializer)
     *     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSerializer.defaultValue)
     * ```
     *
     * @param key 唯一标识该对象的键，请使用 `DataStoreKeys` 中定义的常量。
     * @param serializer 用于序列化/反序列化该对象的 `FileSerializer` 实例。
     * @return 一个 Flow，它会发射存储的对象；或在发生错误/文件为空时，发射 Serializer 的默认值。
     */
    fun <T> getObjectFlow(key: String, serializer: FileSerializer<T>): Flow<T> {
        return getOrCreateDataStore(key, serializer).data
            .catch { exception ->
                // 底层的 Serializer 应该已经处理了 CorruptionException，
                // 这里的 catch 主要是为了捕获更深层次、无法恢复的 IO 错误。
                if (exception is IOException) {
                    emit(serializer.defaultValue)
                } else {
                    throw exception
                }
            }
    }

    /**
     * 一次性地、同步地获取一个可序列化对象。
     *
     * 适用于应用启动初始化、数据迁移、后台任务 (WorkManager) 等非 UI 驱动的场景。
     *
     * ---
     * ### ⚠️ 重要注意事项:
     * - 此函数【必须】在后台线程（如 `Dispatchers.IO`）中调用，否则会阻塞主线程并可能导致 ANR。
     * - 如果发生任何读取或解析错误，此函数会安全地返回 Serializer 的 `defaultValue`，但会包装原始异常并向上抛出 `IllegalStateException` 以便上层记录和监控。
     *
     * ---
     * ### 使用示例 (在后台任务或初始化逻辑中):
     * ```
     * suspend fun getInitialUser(): User {
     *     return withContext(Dispatchers.IO) {
     *         try {
     *             dataStoreManager.getObjectSync(DataStoreKeys.USER_PROFILE, UserSerializer)
     *         } catch (e: IllegalStateException) {
     *             // 记录关键路径的读取失败
     *             errorLogger.log("Failed to load initial user", e)
     *             UserSerializer.defaultValue // 提供最终回退
     *         }
     *     }
     * }
     * ```
     *
     * @param key 唯一标识该对象的键。
     * @param serializer 对象的 `FileSerializer` 实例。
     * @return 返回存储的对象，或在发生错误时抛出异常（内部已安全回退，但外部需感知失败）。
     * @throws IllegalStateException 如果发生任何底层的读取或解析异常。
     */
    suspend fun <T> getObjectSync(key: String, serializer: FileSerializer<T>): T {
        return try {
            getObjectFlow(key, serializer).first()
        } catch (e: Exception) {
            // 包装异常，为上游提供明确的上下文信息，便于日志记录和监控。
            throw IllegalStateException("DataStore synchronous read failed for key=$key", e)
        }
    }

    /**
     * 安全地、原子性地更新一个可序列化对象。
     *
     * 这是更新对象的推荐方式，因为它能保证“读-改-写”操作的原子性，避免了竞态条件。
     *
     * ---
     * ### 注意事项:
     * - `transform` lambda 可能会被多次调用，如果多个写操作同时发生，所以它应该是无副作用的。
     *
     * ---
     * ### 使用示例 (在 ViewModel 或 Repository 中):
     * ```
     * fun incrementUserAge() {
     *     viewModelScope.launch {
     *         dataStoreManager.updateObject(DataStoreKeys.USER_PROFILE, UserSerializer) { currentUser ->
     *             currentUser.copy(age = currentUser.age + 1)
     *         }
     *     }
     * }
     * ```
     *
     * @param key 唯一标识该对象的键。
     * @param serializer 对象的 `FileSerializer` 实例。
     * @param transform 一个挂起函数，接收当前状态 (`t: T`) 并返回新状态 (`T`)。
     */
    suspend fun <T> updateObject(key: String, serializer: FileSerializer<T>, transform: suspend (t: T) -> T) {
        getOrCreateDataStore(key, serializer).updateData(transform)
    }

    /**
     * 用一个新值【完全替换】一个可序列化对象。
     *
     * 这是 `updateObject` 的一个便捷重载，适用于不需要基于当前值进行计算的场景。
     *
     * ---
     * ### 使用示例 (用户登录后，保存全新的用户信息):
     * ```
     * fun onUserLoggedIn(newUser: User) {
     *     viewModelScope.launch {
     *         dataStoreManager.setObject(DataStoreKeys.USER_PROFILE, UserSerializer, newUser)
     *     }
     * }
     * ```
     */
    suspend fun <T> setObject(key: String, serializer: FileSerializer<T>, value: T) {
        updateObject(key, serializer) { value }
    }
}