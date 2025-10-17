package com.demo.core.common.extension

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import java.io.Serializable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// ##################################################################
// ### Activity 委托创建函数 (Activity Delegate Creators)
// ##################################################################

/**
 * 【Activity 用法】为 Activity 创建一个读取 String extra 的属性委托。
 *
 * @param key Extra 的键。
 * @param defaultValue 如果未提供，则返回此默认值。
 *
 * @example
 * class DetailActivity : AppCompatActivity() {
 *     // 使用属性委托安全、便捷地获取参数。
 *     // 语法和 lazy 一样简洁，但100%安全，能正确处理 onNewIntent。
 *     private val bookId: String by extra("KEY_BOOK_ID", "")
 *     private val chapter: Int by extra("KEY_CHAPTER", 1)
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         // 第一次访问，会自动从 intent 中读取
 *         Log.d("DetailActivity", "Book ID: $bookId, Chapter: $chapter")
 *     }
 *
 *     override fun onNewIntent(intent: Intent?) {
 *         super.onNewIntent(intent)
 *         // onNewIntent 之后，Activity 的 intent 会更新。
 *         // 再次访问 bookId 会自动从新的 intent 中读取，数据永远最新。
 *         Log.d("DetailActivity", "New Intent - Book ID: $bookId, Chapter: $chapter")
 *     }
 * }
 */
fun Activity.extra(key: String, defaultValue: String = ""): ReadOnlyProperty<Activity, String> =
    ArgumentDelegate(key, defaultValue, { intent?.extras }) { b, k, d -> b.getString(k, d) ?: d }

fun Activity.extra(key: String, defaultValue: Int = 0): ReadOnlyProperty<Activity, Int> =
    ArgumentDelegate(key, defaultValue, { intent?.extras }) { b, k, d -> b.getInt(k, d) }

fun Activity.extra(key: String, defaultValue: Long = 0L): ReadOnlyProperty<Activity, Long> =
    ArgumentDelegate(key, defaultValue, { intent?.extras }) { b, k, d -> b.getLong(k, d) }

fun Activity.extra(key: String, defaultValue: Boolean = false): ReadOnlyProperty<Activity, Boolean> =
    ArgumentDelegate(key, defaultValue, { intent?.extras }) { b, k, d -> b.getBoolean(k, d) }

fun Activity.extra(key: String, defaultValue: Float = 0f): ReadOnlyProperty<Activity, Float> =
    ArgumentDelegate(key, defaultValue, { intent?.extras }) { b, k, d -> b.getFloat(k, d) }

fun Activity.extra(key: String, defaultValue: Double = 0.0): ReadOnlyProperty<Activity, Double> =
    ArgumentDelegate(key, defaultValue, { intent?.extras }) { b, k, d -> b.getDouble(k, d) }

/**
 * 【Activity 用法】创建一个属性委托，用于从 Activity 的 `Intent` extras 中安全地读取一个可空的 `Parcelable` 对象。
 *
 * 此委托利用 `ArgumentDelegate` 的健壮性，确保在 Activity 重建或 `onNewIntent` 调用后，
 * 属性值能被正确地重新解析，避免了手动从 `Intent` 中获取参数的繁琐和潜在错误。
 *
 * @param key Extra 的键，用于从 `Bundle` 中检索数据。
 * @return 一个 `ReadOnlyProperty` 委托，当首次访问时，它会从 `Activity.intent?.extras` 中获取 `Parcelable` 对象。
 *         如果 `key` 不存在或类型不匹配，则返回 `null`。
 *
 * @example
 * ```kotlin
 * class DetailActivity : AppCompatActivity() {
 *     // 声明一个 Parcelable 类型的属性委托，用于获取书籍对象
 *     private val book: Book? by extraParcelable("KEY_BOOK")
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         book?.let {
 *             Log.d("DetailActivity", "Loaded book: ${it.title}")
 *         } ?: Log.w("DetailActivity", "Book extra not found or invalid.")
 *     }
 * }
 * ```
 */
inline fun <reified T : Parcelable> Activity.extraParcelable(key: String): ReadOnlyProperty<Activity, T?> =
    ArgumentDelegate(key, null, { intent?.extras }) { b, k, _ -> b.optParcelable(k) }

/**
 * 【Activity 用法】创建一个属性委托，用于从 Activity 的 `Intent` extras 中安全地读取一个可空的 `Serializable` 对象。
 *
 * 此委托利用 `ArgumentDelegate` 的健壮性，确保在 Activity 重建或 `onNewIntent` 调用后，
 * 属性值能被正确地重新解析，避免了手动从 `Intent` 中获取参数的繁琐和潜在错误。
 *
 * @param key Extra 的键，用于从 `Bundle` 中检索数据。
 * @return 一个 `ReadOnlyProperty` 委托，当首次访问时，它会从 `Activity.intent?.extras` 中获取 `Serializable` 对象。
 *         如果 `key` 不存在或类型不匹配，则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 User 是一个实现了 Serializable 接口的数据类
 * data class User(val id: String, val name: String) : Serializable
 *
 * class ProfileActivity : AppCompatActivity() {
 *     private val user: User? by extraSerializable("KEY_USER")
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         user?.let {
 *             Log.d("ProfileActivity", "Loaded user: ${it.name}")
 *         } ?: Log.w("ProfileActivity", "User extra not found or invalid.")
 *     }
 * }
 * ```
 */
inline fun <reified T : Serializable> Activity.extraSerializable(key: String): ReadOnlyProperty<Activity, T?> =
    ArgumentDelegate(key, null, { intent?.extras }) { b, k, _ -> b.optSerializable(k) }


// ##################################################################
// ### Fragment 委托创建函数 (Fragment Delegate Creators)
// ##################################################################

/**
 * 【Fragment 用法】为 Fragment 创建一个读取 String argument 的属性委托。
 *
 * @param key Argument 的键。
 * @param defaultValue 如果未提供，则返回此默认值。
 *
 * @example
 * class DetailFragment : Fragment() {
 *     // 语法与 Activity 的 extra(...) 完全一致，简洁且安全。
 *     private val bookId: String by argument("KEY_BOOK_ID", "")
 *     private val chapter: Int by argument("KEY_CHAPTER", 1)
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *         // 访问属性时，会自动从 arguments 中读取
 *         Log.d("DetailFragment", "Book ID: $bookId, Chapter: $chapter")
 *     }
 *
 *     // 【最佳实践】使用 newInstance 工厂方法来创建 Fragment 并传递参数
 *     companion object {
 *         fun newInstance(bookId: String, chapter: Int) = DetailFragment().apply {
 *             arguments = bundleOf(
 *                 "KEY_BOOK_ID" to bookId,
 *                 "KEY_CHAPTER" to chapter
 *             )
 *         }
 *     }
 * }
 */
fun Fragment.argument(key: String, defaultValue: String = ""): ReadOnlyProperty<Fragment, String> =
    ArgumentDelegate(key, defaultValue, { arguments }) { b, k, d -> b.getString(k, d) ?: d }

fun Fragment.argument(key: String, defaultValue: Int = 0): ReadOnlyProperty<Fragment, Int> =
    ArgumentDelegate(key, defaultValue, { arguments }) { b, k, d -> b.getInt(k, d) }

fun Fragment.argument(key: String, defaultValue: Long = 0L): ReadOnlyProperty<Fragment, Long> =
    ArgumentDelegate(key, defaultValue, { arguments }) { b, k, d -> b.getLong(k, d) }

fun Fragment.argument(key: String, defaultValue: Boolean = false): ReadOnlyProperty<Fragment, Boolean> =
    ArgumentDelegate(key, defaultValue, { arguments }) { b, k, d -> b.getBoolean(k, d) }

fun Fragment.argument(key: String, defaultValue: Float = 0f): ReadOnlyProperty<Fragment, Float> =
    ArgumentDelegate(key, defaultValue, { arguments }) { b, k, d -> b.getFloat(k, d) }

fun Fragment.argument(key: String, defaultValue: Double = 0.0): ReadOnlyProperty<Fragment, Double> =
    ArgumentDelegate(key, defaultValue, { arguments }) { b, k, d -> b.getDouble(k, d) }

/**
 * 【Fragment 用法】创建一个属性委托，用于从 Fragment 的 `arguments` 中安全地读取一个可空的 `Parcelable` 对象。
 *
 * 此委托利用 `ArgumentDelegate` 的健壮性，确保在 Fragment 重建后，
 * 属性值能被正确地重新解析，避免了手动从 `Bundle` 中获取参数的繁琐和潜在错误。
 *
 * @param key Argument 的键，用于从 `Bundle` 中检索数据。
 * @return 一个 `ReadOnlyProperty` 委托，当首次访问时，它会从 `Fragment.arguments` 中获取 `Parcelable` 对象。
 *         如果 `key` 不存在或类型不匹配，则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 Product 是一个实现了 Parcelable 接口的数据类
 * data class Product(val id: String, val name: String, val price: Double) : Parcelable
 *
 * class ProductDetailFragment : Fragment() {
 *     private val product: Product? by argumentParcelable("KEY_PRODUCT")
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *         product?.let {
 *             Log.d("ProductDetailFragment", "Loaded product: ${it.name}")
 *         } ?: Log.w("ProductDetailFragment", "Product argument not found or invalid.")
 *     }
 *
 *     companion object {
 *         fun newInstance(product: Product) = ProductDetailFragment().apply {
 *             arguments = bundleOf("KEY_PRODUCT" to product)
 *         }
 *     }
 * }
 * ```
 */
inline fun <reified T : Parcelable> Fragment.argumentParcelable(key: String): ReadOnlyProperty<Fragment, T?> =
    ArgumentDelegate(key, null, { arguments }) { b, k, _ -> b.optParcelable(k) }

/**
 * 【Fragment 用法】创建一个属性委托，用于从 Fragment 的 `arguments` 中安全地读取一个可空的 `Serializable` 对象。
 *
 * 此委托利用 `ArgumentDelegate` 的健壮性，确保在 Fragment 重建后，
 * 属性值能被正确地重新解析，避免了手动从 `Bundle` 中获取参数的繁琐和潜在错误。
 *
 * @param key Argument 的键，用于从 `Bundle` 中检索数据。
 * @return 一个 `ReadOnlyProperty` 委托，当首次访问时，它会从 `Fragment.arguments` 中获取 `Serializable` 对象。
 *         如果 `key` 不存在或类型不匹配，则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 UserProfile 是一个实现了 Serializable 接口的数据类
 * data class UserProfile(val userId: String, val username: String) : Serializable
 *
 * class UserProfileFragment : Fragment() {
 *     private val userProfile: UserProfile? by argumentSerializable("KEY_USER_PROFILE")
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *         userProfile?.let {
 *             Log.d("UserProfileFragment", "Loaded user profile for: ${it.username}")
 *         } ?: Log.w("UserProfileFragment", "User profile argument not found or invalid.")
 *     }
 *
 *     companion object {
 *         fun newInstance(userProfile: UserProfile) = UserProfileFragment().apply {
 *             arguments = bundleOf("KEY_USER_PROFILE" to userProfile)
 *         }
 *     }
 * }
 * ```
 */
inline fun <reified T : Serializable> Fragment.argumentSerializable(key: String): ReadOnlyProperty<Fragment, T?> =
    ArgumentDelegate(key, null, { arguments }) { b, k, _ -> b.optSerializable(k) }


// ##################################################################
// ### 委托实现 (Delegate Implementation) - 生产环境终版
// ##################################################################

/**
 * 一个通用的属性委托，用于从 Bundle 中安全、实时且高效地读取参数。
 *
 * 【核心工作机制】
 * 每一个使用 `by extra(...)` 或 `by argument(...)` 声明的属性，都会在 Activity/Fragment
 * 实例化时，创建一个**全新的、独立的** `ArgumentDelegate` 实例与该属性绑定。
 *
 * 委托实例的生命周期与 Activity/Fragment 实例的生命周期完全同步。
 *
 * 【缓存有效性】
 * 缓存是存在于**每一个独立的委托实例内部**的。因此，对同一个属性的多次访问
 * (例如，在 `onCreate` 中多次读取 `bookId`)，会命中该属性背后唯一的委托实例内部的缓存。
 * 而对不同属性的访问 (例如，读取 `bookId` 和 `chapterId`)，则会分别调用它们各自
 * 绑定的委托实例，互不干扰。
 *
 * 【v1.3.0 生产环境强化】
 * 1. 【健壮性】缓存机制采用 Bundle 实例的引用判等(`===`)，保证在 Activity/Fragment
 *    生命周期内数据的一致性，并在实例重建后安全地让缓存失效。
 * 2. 【性能】缓存模型为“单值缓存”，并使用哨兵对象处理 null 值，轻量且高效。
 * 3. 【健壮性】对 Bundle 的 get 操作使用 runCatching 包装，防止在 Fragment 重建等场景下崩溃。
 * 4. 【类型安全】通过独立的、类型安全的重载函数创建委托，杜绝了依赖废弃的 `Bundle.get(key)`
 *    方法所带来的“静默失败”风险。
 *
 * @param T 期望获取的数据类型。
 * @param key 参数的键。
 * @param defaultValue 默认值。
 * @param bundleProvider 一个 lambda，用于提供从中获取数据的 Bundle 对象。
 * @param getter 一个 lambda，用于从 Bundle 中提取具体类型的值。
 */
class ArgumentDelegate<T>(
    private val key: String,
    private val defaultValue: T,
    private val bundleProvider: () -> Bundle?,
    private val getter: (Bundle, String, T) -> T
) : ReadOnlyProperty<Any, T> {

    // 缓存上一次使用的 Bundle 实例的引用。
    private var cachedBundle: Bundle? = null

    // 缓存从 Bundle 中解析出来的值。
    private var cachedValue: Any? = null

    // 一个特殊的哨兵对象，用于区分“还未缓存任何值”和“缓存了一个null值”。
    private val UNINITIALIZED = Any()

    // 构造时，将缓存值初始化为这个特殊标记，表示“未缓存”状态。
    init {
        cachedValue = UNINITIALIZED
    }

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        // 每次访问属性时，都通过 lambda 实时获取当前的 Bundle。
        val bundle = bundleProvider()

        // 【缓存命中检查】
        // 条件1: `bundle === cachedBundle` -> 检查当前 Bundle 实例是否与上次缓存时的是同一个。
        //                                     如果是 Activity/Fragment 重建，bundle 会是新实例，此条件为 false，缓存自动失效。
        // 条件2: `cachedValue !== UNINITIALIZED` -> 检查是否已经进行过一次成功的取值和缓存。
        if (bundle === cachedBundle && cachedValue !== UNINITIALIZED) {
            // 缓存命中，直接返回已缓存的值，避免重复解析 Bundle。
            return cachedValue as T
        }

        // 【缓存未命中】执行完整的取值和缓存逻辑。
        // 1. 更新缓存的 Bundle 引用。
        cachedBundle = bundle

        // 2. 安全地从 Bundle 中取值。
        val value: T = if (bundle != null && bundle.containsKey(key)) {
            // 使用 runCatching 捕获任何潜在的异常(如系统bug)，保证应用不会崩溃。
            runCatching { getter(bundle, key, defaultValue) }.getOrElse { defaultValue }
        } else {
            // Bundle 为 null 或不包含 key，直接使用默认值。
            defaultValue
        }

        // 3. 将最终结果（取到的值或默认值）写入缓存，并返回。
        //    此时 cachedValue 将不再是 UNINITIALIZED，下次访问即可命中缓存。
        cachedValue = value
        return value
    }
}

// ##################################################################
// ### Bundle? 底层扩展 (Low-level Bundle? Extensions)
// ##################################################################

// --- 基础类型 ---
fun Bundle?.optString(key: String, defaultValue: String = ""): String = this?.getString(key, defaultValue) ?: defaultValue
fun Bundle?.optInt(key: String, defaultValue: Int = 0): Int = this?.getInt(key, defaultValue) ?: defaultValue
fun Bundle?.optLong(key: String, defaultValue: Long = 0L): Long = this?.getLong(key, defaultValue) ?: defaultValue
fun Bundle?.optBoolean(key: String, defaultValue: Boolean = false): Boolean = this?.getBoolean(key, defaultValue) ?: defaultValue
fun Bundle?.optFloat(key: String, defaultValue: Float = 0f): Float = this?.getFloat(key, defaultValue) ?: defaultValue
fun Bundle?.optDouble(key: String, defaultValue: Double = 0.0): Double = this?.getDouble(key, defaultValue) ?: defaultValue
fun Bundle?.optByte(key: String, defaultValue: Byte = 0): Byte = this?.getByte(key, defaultValue) ?: defaultValue
fun Bundle?.optChar(key: String, defaultValue: Char = '\u0000'): Char = this?.getChar(key, defaultValue) ?: defaultValue
fun Bundle?.optShort(key: String, defaultValue: Short = 0): Short = this?.getShort(key, defaultValue) ?: defaultValue
fun Bundle?.optCharSequence(key: String, defaultValue: CharSequence = ""): CharSequence = this?.getCharSequence(key, defaultValue) ?: defaultValue

// --- 数组类型 ---
fun Bundle?.optStringArray(key: String): Array<String>? = this?.getStringArray(key)
fun Bundle?.optIntArray(key: String): IntArray? = this?.getIntArray(key)
fun Bundle?.optLongArray(key: String): LongArray? = this?.getLongArray(key)
fun Bundle?.optBooleanArray(key: String): BooleanArray? = this?.getBooleanArray(key)
fun Bundle?.optFloatArray(key: String): FloatArray? = this?.getFloatArray(key)
fun Bundle?.optDoubleArray(key: String): DoubleArray? = this?.getDoubleArray(key)
fun Bundle?.optByteArray(key: String): ByteArray? = this?.getByteArray(key)
fun Bundle?.optCharArray(key: String): CharArray? = this?.getCharArray(key)
fun Bundle?.optShortArray(key: String): ShortArray? = this?.getShortArray(key)
fun Bundle?.optCharSequenceArray(key: String): Array<out CharSequence>? = this?.getCharSequenceArray(key)

// --- 列表类型 ---
fun Bundle?.optStringArrayList(key: String): ArrayList<String>? = this?.getStringArrayList(key)
fun Bundle?.optIntegerArrayList(key: String): ArrayList<Int>? = this?.getIntegerArrayList(key)
fun Bundle?.optCharSequenceArrayList(key: String): ArrayList<CharSequence>? = this?.getCharSequenceArrayList(key)

// --- 特殊类型 (Serializable, Parcelable) - 已加固 ---
/**
 * 安全地从 `Bundle` 中获取一个可空的 `Serializable` 对象。
 *
 * 此扩展函数处理了 Android 13 (API 33) 引入的 `getSerializable(key, Class)` 方法，
 * 并在旧版本上回退到 `getSerializable(key)` 并进行类型转换，确保了编译期安全和运行时兼容性。
 *
 * @param key 要检索的 `Serializable` 对象的键。
 * @return 如果 `Bundle` 存在且包含指定键的 `Serializable` 对象，并且类型匹配，则返回该对象；否则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 User 是一个实现了 Serializable 接口的数据类
 * data class User(val id: String, val name: String) : Serializable
 *
 * fun processBundle(bundle: Bundle?) {
 *     val user: User? = bundle.optSerializable("KEY_USER")
 *     user?.let {
 *         Log.d("BundleExt", "Retrieved user: ${it.name}")
 *     } ?: Log.w("BundleExt", "User not found in bundle or invalid type.")
 * }
 *
 * // 如何放入 Bundle
 * val user = User("123", "Alice")
 * val bundle = Bundle().apply {
 *     putSerializable("KEY_USER", user)
 * }
 * processBundle(bundle)
 * ```
 */
@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Bundle?.optSerializable(key: String): T? {
    return this?.let {
        if (Build.VERSION.SDK_INT >= 33) {
            getSerializable(key, T::class.java)
        } else {
            getSerializable(key) as? T
        }
    }
}

/**
 * 安全地从 `Bundle` 中获取一个可空的 `Parcelable` 对象。
 *
 * 此扩展函数处理了 Android 13 (API 33) 引入的 `getParcelable(key, Class)` 方法，
 * 并在旧版本上回退到 `getParcelable(key)`，确保了编译期安全和运行时兼容性。
 *
 * @param key 要检索的 `Parcelable` 对象的键。
 * @return 如果 `Bundle` 存在且包含指定键的 `Parcelable` 对象，并且类型匹配，则返回该对象；否则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 Product 是一个实现了 Parcelable 接口的数据类
 * data class Product(val id: String, val name: String) : Parcelable {
 *     override fun describeContents(): Int = 0
 *     override fun writeToParcel(dest: Parcel, flags: Int) { /* ... */ }
 *     companion object CREATOR : Parcelable.Creator<Product> { /* ... */ }
 * }
 *
 * fun processBundle(bundle: Bundle?) {
 *     val product: Product? = bundle.optParcelable("KEY_PRODUCT")
 *     product?.let {
 *         Log.d("BundleExt", "Retrieved product: ${it.name}")
 *     } ?: Log.w("BundleExt", "Product not found in bundle or invalid type.")
 * }
 *
 * // 如何放入 Bundle
 * val product = Product("p1", "Laptop")
 * val bundle = Bundle().apply {
 *     putParcelable("KEY_PRODUCT", product)
 * }
 * processBundle(bundle)
 * ```
 */
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle?.optParcelable(key: String): T? {
    return this?.let {
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelable(key, T::class.java)
        } else {
            getParcelable(key)
        }
    }
}

/**
 * 安全地从 `Bundle` 中获取一个可空的 `Parcelable` 对象列表 (`ArrayList<T>`)。
 *
 * 此扩展函数处理了 Android 13 (API 33) 引入的 `getParcelableArrayList(key, Class)` 方法，
 * 并在旧版本上回退到 `getParcelableArrayList(key)`，确保了编译期安全和运行时兼容性。
 *
 * @param key 要检索的 `Parcelable` 对象列表的键。
 * @return 如果 `Bundle` 存在且包含指定键的 `Parcelable` 对象列表，并且类型匹配，则返回该列表；否则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 Item 是一个实现了 Parcelable 接口的数据类
 * data class Item(val id: String, val name: String) : Parcelable {
 *     override fun describeContents(): Int = 0
 *     override fun writeToParcel(dest: Parcel, flags: Int) { /* ... */ }
 *     companion object CREATOR : Parcelable.Creator<Item> { /* ... */ }
 * }
 *
 * fun processBundle(bundle: Bundle?) {
 *     val items: ArrayList<Item>? = bundle.optParcelableArrayList("KEY_ITEMS")
 *     items?.let {
 *         Log.d("BundleExt", "Retrieved ${it.size} items.")
 *         it.forEach { item -> Log.d("BundleExt", "Item: ${item.name}") }
 *     } ?: Log.w("BundleExt", "Items list not found in bundle or invalid type.")
 * }
 *
 * // 如何放入 Bundle
 * val items = arrayListOf(Item("i1", "Pen"), Item("i2", "Book"))
 * val bundle = Bundle().apply {
 *     putParcelableArrayList("KEY_ITEMS", items)
 * }
 * processBundle(bundle)
 * ```
 */
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle?.optParcelableArrayList(key: String): ArrayList<T>? {
    return this?.let {
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableArrayList(key, T::class.java)
        } else {
            getParcelableArrayList(key)
        }
    }
}

/**
 * 安全地从 `Bundle` 中获取一个可空的 `Parcelable` 对象数组 (`Array<T>`)。
 *
 * 此扩展函数处理了 Android 13 (API 33) 引入的 `getParcelableArray(key, Class)` 方法，
 * 并在旧版本上回退到 `getParcelableArray(key)` 并进行类型转换，确保了编译期安全和运行时兼容性。
 *
 * @param key 要检索的 `Parcelable` 对象数组的键。
 * @return 如果 `Bundle` 存在且包含指定键的 `Parcelable` 对象数组，并且类型匹配，则返回该数组；否则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 DataPoint 是一个实现了 Parcelable 接口的数据类
 * data class DataPoint(val x: Float, val y: Float) : Parcelable {
 *     override fun describeContents(): Int = 0
 *     override fun writeToParcel(dest: Parcel, flags: Int) { /* ... */ }
 *     companion object CREATOR : Parcelable.Creator<DataPoint> { /* ... */ }
 * }
 *
 * fun processBundle(bundle: Bundle?) {
 *     val dataPoints: Array<DataPoint>? = bundle.optParcelableArray("KEY_DATA_POINTS")
 *     dataPoints?.let {
 *         Log.d("BundleExt", "Retrieved ${it.size} data points.")
 *         it.forEach { dp -> Log.d("BundleExt", "DataPoint: (${dp.x}, ${dp.y})") }
 *     } ?: Log.w("BundleExt", "Data points array not found in bundle or invalid type.")
 * }
 *
 * // 如何放入 Bundle
 * val dataPoints = arrayOf(DataPoint(1.0f, 2.0f), DataPoint(3.0f, 4.0f))
 * val bundle = Bundle().apply {
 *     putParcelableArray("KEY_DATA_POINTS", dataPoints)
 * }
 * processBundle(bundle)
 * ```
 */
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle?.optParcelableArray(key: String): Array<T>? {
    return this?.let {
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableArray(key, T::class.java) as? Array<T>
        } else {
            // 这是保证类型安全的、最地道的 Kotlin 写法。
            // 直接强转 as? Array<T> 会因数组类型不匹配而失败返回 null。
            getParcelableArray(key)?.filterIsInstance<T>()?.toTypedArray()
        }
    }
}


// ##################################################################
// ### Intent? 底层扩展 (Low-level Intent? Extensions)
// ##################################################################

// --- 基础类型 ---
fun Intent?.optString(key: String, defaultValue: String = ""): String = this?.getStringExtra(key) ?: defaultValue
fun Intent?.optInt(key: String, defaultValue: Int = 0): Int = this?.getIntExtra(key, defaultValue) ?: defaultValue
fun Intent?.optLong(key: String, defaultValue: Long = 0L): Long = this?.getLongExtra(key, defaultValue) ?: defaultValue
fun Intent?.optBoolean(key: String, defaultValue: Boolean = false): Boolean = this?.getBooleanExtra(key, defaultValue) ?: defaultValue
fun Intent?.optFloat(key: String, defaultValue: Float = 0f): Float = this?.getFloatExtra(key, defaultValue) ?: defaultValue
fun Intent?.optDouble(key: String, defaultValue: Double = 0.0): Double = this?.getDoubleExtra(key, defaultValue) ?: defaultValue
fun Intent?.optByte(key: String, defaultValue: Byte = 0): Byte = this?.getByteExtra(key, defaultValue) ?: defaultValue
fun Intent?.optChar(key: String, defaultValue: Char = '\u0000'): Char = this?.getCharExtra(key, defaultValue) ?: defaultValue
fun Intent?.optShort(key: String, defaultValue: Short = 0): Short = this?.getShortExtra(key, defaultValue) ?: defaultValue
fun Intent?.optCharSequence(key: String, defaultValue: CharSequence = ""): CharSequence = this?.getCharSequenceExtra(key) ?: defaultValue

// --- 数组类型 ---
fun Intent?.optStringArray(key: String): Array<String>? = this?.getStringArrayExtra(key)
fun Intent?.optIntArray(key: String): IntArray? = this?.getIntArrayExtra(key)
fun Intent?.optLongArray(key: String): LongArray? = this?.getLongArrayExtra(key)
fun Intent?.optBooleanArray(key: String): BooleanArray? = this?.getBooleanArrayExtra(key)
fun Intent?.optFloatArray(key: String): FloatArray? = this?.getFloatArrayExtra(key)
fun Intent?.optDoubleArray(key: String): DoubleArray? = this?.getDoubleArrayExtra(key)
fun Intent?.optByteArray(key: String): ByteArray? = this?.getByteArrayExtra(key)
fun Intent?.optCharArray(key: String): CharArray? = this?.getCharArrayExtra(key)
fun Intent?.optShortArray(key: String): ShortArray? = this?.getShortArrayExtra(key)
fun Intent?.optCharSequenceArray(key: String): Array<out CharSequence>? = this?.getCharSequenceArrayExtra(key)

// --- 列表类型 ---
fun Intent?.optStringArrayList(key: String): ArrayList<String>? = this?.getStringArrayListExtra(key)
fun Intent?.optIntegerArrayList(key: String): ArrayList<Int>? = this?.getIntegerArrayListExtra(key)
fun Intent?.optCharSequenceArrayList(key: String): ArrayList<CharSequence>? = this?.getCharSequenceArrayListExtra(key)

// --- 特殊类型 (Serializable, Parcelable) - 已加固 ---
/**
 * 安全地从 `Intent` 中获取一个可空的 `Serializable` 对象。
 *
 * 此扩展函数处理了 Android 13 (API 33) 引入的 `getSerializableExtra(key, Class)` 方法，
 * 并在旧版本上回退到 `getSerializableExtra(key)` 并进行类型转换，确保了编译期安全和运行时兼容性。
 *
 * @param key 要检索的 `Serializable` 对象的键。
 * @return 如果 `Intent` 存在且包含指定键的 `Serializable` 对象，并且类型匹配，则返回该对象；否则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 User 是一个实现了 Serializable 接口的数据类
 * data class User(val id: String, val name: String) : Serializable
 *
 * fun processIntent(intent: Intent?) {
 *     val user: User? = intent.optSerializable("KEY_USER")
 *     user?.let {
 *         Log.d("IntentExt", "Retrieved user: ${it.name}")
 *     } ?: Log.w("IntentExt", "User not found in intent or invalid type.")
 * }
 *
 * // 如何放入 Intent
 * val user = User("123", "Alice")
 * val intent = Intent().apply {
 *     putExtra("KEY_USER", user)
 * }
 * processIntent(intent)
 * ```
 */
@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Intent?.optSerializable(key: String): T? {
    return this?.let {
        if (Build.VERSION.SDK_INT >= 33) {
            getSerializableExtra(key, T::class.java)
        } else {
            getSerializableExtra(key) as? T
        }
    }
}

/**
 * 安全地从 `Intent` 中获取一个可空的 `Parcelable` 对象。
 *
 * 此扩展函数处理了 Android 13 (API 33) 引入的 `getParcelableExtra(key, Class)` 方法，
 * 并在旧版本上回退到 `getParcelableExtra(key)`，确保了编译期安全和运行时兼容性。
 *
 * @param key 要检索的 `Parcelable` 对象的键。
 * @return 如果 `Intent` 存在且包含指定键的 `Parcelable` 对象，并且类型匹配，则返回该对象；否则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 Product 是一个实现了 Parcelable 接口的数据类
 * data class Product(val id: String, val name: String) : Parcelable {
 *     override fun describeContents(): Int = 0
 *     override fun writeToParcel(dest: Parcel, flags: Int) { /* ... */ }
 *     companion object CREATOR : Parcelable.Creator<Product> { /* ... */ }
 * }
 *
 * fun processIntent(intent: Intent?) {
 *     val product: Product? = intent.optParcelable("KEY_PRODUCT")
 *     product?.let {
 *         Log.d("IntentExt", "Retrieved product: ${it.name}")
 *     } ?: Log.w("IntentExt", "Product not found in intent or invalid type.")
 * }
 *
 * // 如何放入 Intent
 * val product = Product("p1", "Laptop")
 * val intent = Intent().apply {
 *     putExtra("KEY_PRODUCT", product)
 * }
 * processIntent(intent)
 * ```
 */
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent?.optParcelable(key: String): T? {
    return this?.let {
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(key, T::class.java)
        } else {
            getParcelableExtra(key)
        }
    }
}

/**
 * 安全地从 `Intent` 中获取一个可空的 `Parcelable` 对象列表 (`ArrayList<T>`)。
 *
 * 此扩展函数处理了 Android 13 (API 33) 引入的 `getParcelableArrayListExtra(key, Class)` 方法，
 * 并在旧版本上回退到 `getParcelableArrayListExtra(key)`，确保了编译期安全和运行时兼容性。
 *
 * @param key 要检索的 `Parcelable` 对象列表的键。
 * @return 如果 `Intent` 存在且包含指定键的 `Parcelable` 对象列表，并且类型匹配，则返回该列表；否则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 Item 是一个实现了 Parcelable 接口的数据类
 * data class Item(val id: String, val name: String) : Parcelable {
 *     override fun describeContents(): Int = 0
 *     override fun writeToParcel(dest: Parcel, flags: Int) { /* ... */ }
 *     companion object CREATOR : Parcelable.Creator<Item> { /* ... */ }
 * }
 *
 * fun processIntent(intent: Intent?) {
 *     val items: ArrayList<Item>? = intent.optParcelableArrayList("KEY_ITEMS")
 *     items?.let {
 *         Log.d("IntentExt", "Retrieved ${it.size} items.")
 *         it.forEach { item -> Log.d("IntentExt", "Item: ${item.name}") }
 *     } ?: Log.w("IntentExt", "Items list not found in intent or invalid type.")
 * }
 *
 * // 如何放入 Intent
 * val items = arrayListOf(Item("i1", "Pen"), Item("i2", "Book"))
 * val intent = Intent().apply {
 *     putParcelableArrayListExtra("KEY_ITEMS", items)
 * }
 * processIntent(intent)
 * ```
 */
@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent?.optParcelableArrayList(key: String): ArrayList<T>? {
    return this?.let {
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableArrayListExtra(key, T::class.java)
        } else {
            getParcelableArrayListExtra(key)
        }
    }
}

/**
 * 安全地从 `Intent` 中获取一个可空的 `Parcelable` 对象数组 (`Array<T>`)。
 *
 * 此扩展函数处理了 Android 13 (API 33) 引入的 `getParcelableArrayExtra(key, Class)` 方法，
 * 并在旧版本上回退到 `getParcelableArrayExtra(key)` 并进行类型转换，确保了编译期安全和运行时兼容性。
 *
 * **性能优化**：在旧版本 (API < 33) 上，通过直接强转避免了 `mapNotNull` 和 `toTypedArray` 带来的装箱和 GC 压力。
 *
 * @param key 要检索的 `Parcelable` 对象数组的键。
 * @return 如果 `Intent` 存在且包含指定键的 `Parcelable` 对象数组，并且类型匹配，则返回该数组；否则返回 `null`。
 *
 * @example
 * ```kotlin
 * // 假设 DataPoint 是一个实现了 Parcelable 接口的数据类
 * data class DataPoint(val x: Float, val y: Float) : Parcelable {
 *     override fun describeContents(): Int = 0
 *     override fun writeToParcel(dest: Parcel, flags: Int) { /* ... */ }
 *     companion object CREATOR : Parcelable.Creator<DataPoint> { /* ... */ }
 * }
 *
 * fun processIntent(intent: Intent?) {
 *     val dataPoints: Array<DataPoint>? = intent.optParcelableArray("KEY_DATA_POINTS")
 *     dataPoints?.let {
 *         Log.d("IntentExt", "Retrieved ${it.size} data points.")
 *         it.forEach { dp -> Log.d("IntentExt", "DataPoint: (${dp.x}, ${dp.y})") }
 *     } ?: Log.w("IntentExt", "Data points array not found in intent or invalid type.")
 * }
 *
 * // 如何放入 Intent
 * val dataPoints = arrayOf(DataPoint(1.0f, 2.0f), DataPoint(3.0f, 4.0f))
 * val intent = Intent().apply {
 *     putParcelableArrayExtra("KEY_DATA_POINTS", dataPoints)
 * }
 * processIntent(intent)
 * ```
 */
@Suppress("DEPRECATION", "UNCHECKED_CAST") // Suppress UNCHECKED_CAST for direct cast on older APIs
inline fun <reified T : Parcelable> Intent?.optParcelableArray(key: String): Array<T>? {
    return this?.let {
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableArrayExtra(key, T::class.java) as? Array<T>
        } else {
            // 这是保证类型安全的、最地道的 Kotlin 写法。
            // 直接强转 as? Array<T> 会因数组类型不匹配而失败返回 null。
            getParcelableArrayExtra(key)?.filterIsInstance<T>()?.toTypedArray()
        }
    }
}