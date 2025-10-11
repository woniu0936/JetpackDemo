// Filename: ReactiveMMKV.kt
@file:Suppress("UNCHECKED_CAST")

package com.demo.core.mmkv

import android.content.Context
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.getValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// 内部事件总线，用于在 MMKV 值变化时发出通知。
/**
 * [V13 - Kotlinx Serialization] 一个高性能、线程安全、响应式的 MMKV 封装库。
 *
 * 此版本使用 kotlinx.serialization 作为 JSON 序列化引擎，提供了与 Kotlin 语言的最佳集成。
 * 它通过函数重载处理原生类型，简化了 API，同时通过挂起函数和 Flow 保证了线程安全。
 *
 * **核心要求**: 所有需要存储的自定义数据类都必须添加 `@Serializable` 注解。
 *
 * @Serializable
 * data class User(val name: String, val age: Int)
 */
@Singleton
class ReactiveMMKV @Inject constructor(
    private val mmkv: MMKV,
    private val eventBus: MMKVEventBus
) {

    companion object {
        /**
         * 全局共享、可配置的 Json 实例，用于所有序列化/反序列化操作。
         * - ignoreUnknownKeys = true: 如果 JSON 中有 Kotlin 类没有的字段，则忽略，增强兼容性。
         * - coerceInputValues = true: 如果 JSON 中的值类型与 Kotlin 类不匹配（例如 null 对应非空类型），则尝试强制转换为默认值。
         */
        val json: Json by lazy {
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
        }

//        @Volatile
//        private var INSTANCE: ReactiveMMKV? = null
//
//        /**
//         * 获取 ReactiveMMKV 的全局单例。
//         *
//         * @return ReactiveMMKV 的单例实例。
//         *
//         * ### 使用示例:
//         * ```
//         * // 在 Application 或依赖注入模块中
//         * val mmkv = ReactiveMMKV.getInstance()
//         * ```
//         */
//        fun getInstance(): ReactiveMMKV {
//            return INSTANCE ?: synchronized(this) {
//                INSTANCE ?: ReactiveMMKV(MMKV.defaultMMKV()).also { INSTANCE = it }
//            }
//        }
//
//        /**
//         * 获取 ReactiveMMKV 的全局单例，并在首次调用时初始化 MMKV。
//         * 这是一个线程安全的双重检查锁定实现。
//         *
//         * @param context ApplicationContext，仅在首次初始化时需要。
//         * @return ReactiveMMKV 的单例实例。
//         *
//         * ### 使用示例:
//         * ```
//         * // 在 Application.onCreate 中
//         * class MyApplication : Application() {
//         *     override fun onCreate() {
//         *         super.onCreate()
//         *         ReactiveMMKV.getInstance(this)
//         *     }
//         * }
//         *
//         * // 在其他地方获取实例
//         * val mmkv = ReactiveMMKV.getInstance(context)
//         * ```
//         */
//        fun getInstance(context: Context): ReactiveMMKV {
//            return INSTANCE ?: synchronized(this) {
//                INSTANCE ?: run {
//                    MMKV.initialize(context.applicationContext)
//                    ReactiveMMKV(MMKV.defaultMMKV()).also { INSTANCE = it }
//                }
//            }
//        }
    }

    /* ======================================================================================= */
    /* ===                 原生类型 API (使用函数重载)                                       === */
    /* ======================================================================================= */

    /**
     * 存储一个 String? 类型的值。此操作是同步的，但速度极快。
     * @param key 键。
     * @param value 要存储的值。
     *
     * ### 使用示例:
     * ```
     * mmkv.put("user_token", "xyz-abc-123")
     * mmkv.put("user_token", null) // 存储 null 会移除该键
     * ```
     */
    fun put(key: String, value: String?) = mmkv.encode(key, value).also { notify(key, value) }
    fun put(key: String, value: Int) = mmkv.encode(key, value).also { notify(key, value) }
    fun put(key: String, value: Boolean) = mmkv.encode(key, value).also { notify(key, value) }
    fun put(key: String, value: Long) = mmkv.encode(key, value).also { notify(key, value) }
    fun put(key: String, value: Float) = mmkv.encode(key, value).also { notify(key, value) }
    fun put(key: String, value: Double) = mmkv.encode(key, value).also { notify(key, value) }
    fun put(key: String, value: ByteArray?) = mmkv.encode(key, value).also { notify(key, value) }
    fun put(key: String, value: Set<String>?) = mmkv.encode(key, value).also { notify(key, value) }

    /**
     * [挂起函数] 异步地获取一个 String? 类型的值，保证在后台线程执行 I/O 操作。
     * @param key 键。
     * @param defaultValue 如果键不存在时返回的默认值。
     * @return 存储的值或默认值。
     *
     * ### 使用示例:
     * ```
     * viewModelScope.launch {
     *     val token = mmkv.get("user_token", null)
     *     if (token != null) {
     *         // ...
     *     }
     * }
     * ```
     */
    suspend fun get(key: String, defaultValue: String?): String? = withContext(Dispatchers.IO) { mmkv.decodeString(key, defaultValue) }
    suspend fun get(key: String, defaultValue: Int): Int = withContext(Dispatchers.IO) { mmkv.decodeInt(key, defaultValue) }
    suspend fun get(key: String, defaultValue: Boolean): Boolean = withContext(Dispatchers.IO) { mmkv.decodeBool(key, defaultValue) }
    suspend fun get(key: String, defaultValue: Long): Long = withContext(Dispatchers.IO) { mmkv.decodeLong(key, defaultValue) }
    suspend fun get(key: String, defaultValue: Float): Float = withContext(Dispatchers.IO) { mmkv.decodeFloat(key, defaultValue) }
    suspend fun get(key: String, defaultValue: Double): Double = withContext(Dispatchers.IO) { mmkv.decodeDouble(key, defaultValue) }
    suspend fun get(key: String, defaultValue: ByteArray?): ByteArray? = withContext(Dispatchers.IO) { mmkv.decodeBytes(key, defaultValue) }
    suspend fun get(key: String, defaultValue: Set<String>?): Set<String>? = withContext(Dispatchers.IO) { mmkv.decodeStringSet(key, defaultValue) }

    /**
     * [Flow API] 响应式地观察一个 String? 类型的值。
     * @param key 键。
     * @param defaultValue 默认值。
     * @return 一个会随值变化而发射新值的 Flow。
     *
     * ### 使用示例:
     * ```
     * val tokenFlow: Flow<String?> = mmkv.getFlow("user_token", null)
     *
     * tokenFlow.collect { token ->
     *     // 当 "user_token" 的值变化时，这里会收到通知
     * }
     * ```
     */
    fun getFlow(key: String, defaultValue: String?): Flow<String?> = getFlowInternal(key, defaultValue)
    fun getFlow(key: String, defaultValue: Int): Flow<Int> = getFlowInternal(key, defaultValue)
    fun getFlow(key: String, defaultValue: Boolean): Flow<Boolean> = getFlowInternal(key, defaultValue)
    fun getFlow(key: String, defaultValue: Long): Flow<Long> = getFlowInternal(key, defaultValue)
    fun getFlow(key: String, defaultValue: Float): Flow<Float> = getFlowInternal(key, defaultValue)
    fun getFlow(key: String, defaultValue: Double): Flow<Double> = getFlowInternal(key, defaultValue)
    fun getFlow(key: String, defaultValue: ByteArray?): Flow<ByteArray?> = getFlowInternal(key, defaultValue)
    fun getFlow(key: String, defaultValue: Set<String>?): Flow<Set<String>?> = getFlowInternal(key, defaultValue)


    /* ======================================================================================= */
    /* ===                      复杂对象、列表和 Map API                                     === */
    /* ======================================================================================= */

    /**
     * [挂起函数] 将任何添加了 `@Serializable` 注解的对象序列化为 JSON 字符串并存储。
     * @param T 对象的类型，必须是 @Serializable。
     * @param key 键。
     * @param value 要存储的对象。如果为 null，则会移除该键。
     * @param serializer 对象的序列化器。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class User(val name: String)
     *
     * viewModelScope.launch {
     *     val user = User("Alice")
     *     mmkv.putObject("current_user", user, User.serializer())
     * }
     * ```
     */
    suspend fun <T> putObject(key: String, value: T?, serializer: KSerializer<T>) {
        if (value == null) {
            remove(key)
            return
        }
        val jsonString = withContext(Dispatchers.IO) { json.encodeToString(serializer, value) }
        put(key, jsonString)
    }

    /**
     * [挂起函数] 存储对象的便利版本，自动推断序列化器。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class User(val name: String)
     *
     * viewModelScope.launch {
     *     mmkv.putObject("current_user", User("Bob"))
     * }
     * ```
     */
    suspend inline fun <reified T> putObject(key: String, value: T?) {
        putObject(key, value, serializer())
    }

    /**
     * [挂起函数] 异步地获取并反序列化一个 JSON 字符串为对象。
     * @param T 对象的类型。
     * @param key 键。
     * @param serializer 对象的序列化器。
     * @param defaultValue 默认值。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class User(val name: String)
     *
     * viewModelScope.launch {
     *     val defaultUser = User("Guest")
     *     val user = mmkv.getObject("current_user", User.serializer(), defaultUser)
     *     // use user
     * }
     * ```
     */
    suspend fun <T> getObject(key: String, serializer: KSerializer<T>, defaultValue: T): T {
        return withContext(Dispatchers.IO) {
            val jsonString = mmkv.decodeString(key)
            if (jsonString.isNullOrEmpty()) defaultValue else {
                try {
                    json.decodeFromString(serializer, jsonString)
                } catch (e: Exception) {
                    defaultValue
                }
            }
        }
    }

    /**
     * [挂起函数] 获取对象的便利版本，自动推断序列化器。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class User(val name: String)
     *
     * viewModelScope.launch {
     *     val user = mmkv.getObject<User?>("current_user", null)
     * }
     * ```
     */
    suspend inline fun <reified T> getObject(key: String, defaultValue: T): T {
        return getObject(key, serializer(), defaultValue)
    }

    /**
     * [Flow API] 响应式地观察一个复杂对象。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class User(val name: String)
     *
     * val userFlow: Flow<User?> = mmkv.getObjectFlow("user", User.serializer(), null)
     * ```
     */
    fun <T> getObjectFlow(key: String, serializer: KSerializer<T>, defaultValue: T): Flow<T> {
        return getFlow(key, null as String?)
            .map { jsonString ->
                if (jsonString.isNullOrEmpty()) defaultValue else {
                    try {
                        json.decodeFromString(serializer, jsonString)
                    } catch (e: Exception) {
                        defaultValue
                    }
                }
            }
    }

    /**
     * [Flow API] 观察复杂对象的便利版本，自动推断序列化器。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class User(val name: String)
     *
     * val userFlow: Flow<User?> = mmkv.getObjectFlow("user", null)
     * ```
     */
    inline fun <reified T> getObjectFlow(key: String, defaultValue: T): Flow<T> {
        return getObjectFlow(key, serializer(), defaultValue)
    }

    /**
     * [挂起函数] 异步地获取一个对象列表。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class Tag(val name: String)
     *
     * viewModelScope.launch {
     *     val tags = mmkv.getList<Tag>("user_tags")
     * }
     * ```
     */
    suspend inline fun <reified T> getList(key: String, defaultValue: List<T> = emptyList()): List<T> {
        return getObject(key, defaultValue)
    }

    /**
     * [Flow API] 响应式地观察一个对象列表。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class Tag(val name: String)
     *
     * val tagsFlow: Flow<List<Tag>> = mmkv.getListFlow("user_tags")
     * ```
     */
    inline fun <reified T> getListFlow(key: String, defaultValue: List<T> = emptyList()): Flow<List<T>> {
        return getObjectFlow(key, defaultValue)
    }

    /**
     * [挂起函数] 以原子方式编辑一个列表。这是修改列表的推荐方式。
     * 它会先读取现有列表，在你的 lambda 中进行转换，然后将新列表写回。
     *
     * @param transform 一个 lambda，接收当前列表并返回一个新列表。
     *
     * ### 使用示例:
     * ```
     * @Serializable data class Tag(val name: String)
     *
     * viewModelScope.launch {
     *     mmkv.editList<Tag>("user_tags") { currentList ->
     *         (currentList + Tag("new_tag")).distinct() // 添加新标签并去重
     *     }
     * }
     * ```
     */
    suspend inline fun <reified T> editList(key: String, crossinline transform: (list: List<T>) -> List<T>) {
        val currentList = getList<T>(key)
        val newList = transform(currentList)
        putObject(key, newList)
    }

    /**
     * [挂起函数] 异步地获取一个 Map。
     */
    suspend inline fun <reified K, reified V> getMap(key: String, defaultValue: Map<K, V> = emptyMap()): Map<K, V> {
        return getObject(key, defaultValue)
    }

    /**
     * [Flow API] 响应式地观察一个 Map。
     */
    inline fun <reified K, reified V> getMapFlow(key: String, defaultValue: Map<K, V> = emptyMap()): Flow<Map<K, V>> {
        return getObjectFlow(key, defaultValue)
    }

    /**
     * [挂起函数] 以原子方式编辑一个 Map。
     */
    suspend inline fun <reified K, reified V> editMap(key: String, crossinline transform: (map: Map<K, V>) -> Map<K, V>) {
        val currentMap = getMap<K, V>(key)
        val newMap = transform(currentMap)
        putObject(key, newMap)
    }

    /* ======================================================================================= */
    /* ===                           通用及内部实现                                          === */
    /* ======================================================================================= */

    /**
     * 移除一个键及其对应的值。
     *
     * ### 使用示例:
     * ```
     * mmkv.remove("user_token")
     * ```
     */
    fun remove(key: String) {
        mmkv.removeValueForKey(key)
        notify(key, null)
    }

    private fun notify(key: String, value: Any?) {
        eventBus.notify(key, value)
    }

    private fun <T> getFlowInternal(key: String, defaultValue: T): Flow<T> =
        eventBus.events
            .filter { it.first == key }
            .map { it.second as? T ?: defaultValue }
            .onStart {
                val currentValue = when (defaultValue) {
                    is String? -> mmkv.decodeString(key, defaultValue)
                    is Int -> mmkv.decodeInt(key, defaultValue)
                    is Boolean -> mmkv.decodeBool(key, defaultValue)
                    is Long -> mmkv.decodeLong(key, defaultValue)
                    is Float -> mmkv.decodeFloat(key, defaultValue)
                    is Double -> mmkv.decodeDouble(key, defaultValue)
                    is ByteArray? -> mmkv.decodeBytes(key, defaultValue)
                    is Set<*>? -> mmkv.decodeStringSet(key, defaultValue as? Set<String>)
                    else -> defaultValue
                }
                emit(currentValue as T)
            }
            .distinctUntilChanged()
}

// 内部辅助类，用于实现委托属性。
private class MMKVPreferenceDelegate<T>(
    private val key: String,
    private val defaultValue: T,
    private val getter: suspend (String, T) -> T,
    private val setter: (String, T) -> Unit
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        // 警告: 此操作会在当前线程阻塞，直到后台的 get 操作完成。
        // 强烈建议只在确定处于后台线程的模块（如 Repository）中使用。
        return runBlocking {
            getter(key, defaultValue)
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setter(key, value)
    }
}

/**
 * 创建一个 Int 类型的委托属性。
 *
 * **警告**: 在主线程上读取此属性可能会导致 ANR。请只在后台模块中使用。
 *
 * ### 使用示例 (在 Repository 中):
 * ```
 * class ConfigRepo(mmkv: ReactiveMMKV) {
 *     // 在后台线程访问是安全的
 *     var retryCount: Int by mmkv.preference("retry_count", 3)
 * }
 * ```
 */
fun ReactiveMMKV.preference(key: String, defaultValue: Int): ReadWriteProperty<Any?, Int> =
    MMKVPreferenceDelegate(key, defaultValue, this::get, this::put)

fun ReactiveMMKV.preference(key: String, defaultValue: String?): ReadWriteProperty<Any?, String?> =
    MMKVPreferenceDelegate(key, defaultValue, this::get, this::put)

fun ReactiveMMKV.preference(key: String, defaultValue: Boolean): ReadWriteProperty<Any?, Boolean> =
    MMKVPreferenceDelegate(key, defaultValue, this::get, this::put)

fun ReactiveMMKV.preference(key: String, defaultValue: Long): ReadWriteProperty<Any?, Long> =
    MMKVPreferenceDelegate(key, defaultValue, this::get, this::put)

fun ReactiveMMKV.preference(key: String, defaultValue: Float): ReadWriteProperty<Any?, Float> =
    MMKVPreferenceDelegate(key, defaultValue, this::get, this::put)

fun ReactiveMMKV.preference(key: String, defaultValue: Double): ReadWriteProperty<Any?, Double> =
    MMKVPreferenceDelegate(key, defaultValue, this::get, this::put)

fun ReactiveMMKV.preference(key: String, defaultValue: ByteArray?): ReadWriteProperty<Any?, ByteArray?> =
    MMKVPreferenceDelegate(key, defaultValue, this::get, this::put)

fun ReactiveMMKV.preference(key: String, defaultValue: Set<String>?): ReadWriteProperty<Any?, Set<String>?> =
    MMKVPreferenceDelegate(key, defaultValue, this::get, this::put)

/* ======================================================================================= */
/* ===                 [安全] 响应式 API 扩展 (别名)                                     === */
/* ======================================================================================= */

/**
 * [安全] 响应式地观察一个 Int 值。这是 `getFlow` 的一个更符合语义的别名。
 *
 * ### 使用示例 (在 ViewModel 中):
 * ```
 * val notificationCount: StateFlow<Int> = mmkv.observe("notification_count", 0)
 *     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
 * ```
 */
fun ReactiveMMKV.observe(key: String, defaultValue: Int): Flow<Int> =
    this.getFlow(key, defaultValue)

fun ReactiveMMKV.observe(key: String, defaultValue: String?): Flow<String?> =
    this.getFlow(key, defaultValue)

fun ReactiveMMKV.observe(key: String, defaultValue: Boolean): Flow<Boolean> =
    this.getFlow(key, defaultValue)

fun ReactiveMMKV.observe(key: String, defaultValue: Long): Flow<Long> =
    this.getFlow(key, defaultValue)

fun ReactiveMMKV.observe(key: String, defaultValue: Float): Flow<Float> =
    this.getFlow(key, defaultValue)

fun ReactiveMMKV.observe(key: String, defaultValue: Double): Flow<Double> =
    this.getFlow(key, defaultValue)

fun ReactiveMMKV.observe(key: String, defaultValue: ByteArray?): Flow<ByteArray?> =
    this.getFlow(key, defaultValue)

fun ReactiveMMKV.observe(key: String, defaultValue: Set<String>?): Flow<Set<String>?> =
    this.getFlow(key, defaultValue)

/**
 * [安全] 响应式地观察一个复杂对象。
 *
 * ### 使用示例 (在 ViewModel 中):
 * ```
 * @Serializable data class User(val name: String)
 *
 * val userFlow: StateFlow<User?> = mmkv.observeObject<User>("current_user")
 *     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
 * ```
 */
inline fun <reified T> ReactiveMMKV.observeObject(key: String, defaultValue: T? = null): Flow<T?> {
    return this.getObjectFlow(key, defaultValue)
}

/**
 * [安全] 响应式地观察一个列表。
 *
 * ### 使用示例 (在 ViewModel 中):
 * ```
 * @Serializable data class Tag(val name: String)
 *
 * val tagsFlow: StateFlow<List<Tag>> = mmkv.observeList<Tag>("user_tags")
 *     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
 * ```
 */
inline fun <reified T> ReactiveMMKV.observeList(
    key: String,
    defaultValue: List<T> = emptyList()
): Flow<List<T>> {
    return this.getListFlow(key, defaultValue)
}

/**
 * [安全] 响应式地观察一个 Map。
 *
 * ### 使用示例 (在 ViewModel 中):
 * ```
 * val progressFlow: StateFlow<Map<String, Int>> = mmkv.observeMap<String, Int>("course_progress")
 *     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())
 * ```
 */
inline fun <reified K, reified V> ReactiveMMKV.observeMap(
    key: String,
    defaultValue: Map<K, V> = emptyMap()
): Flow<Map<K, V>> {
    return this.getMapFlow(key, defaultValue)
}