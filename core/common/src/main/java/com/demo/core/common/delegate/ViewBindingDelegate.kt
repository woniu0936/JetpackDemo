package com.demo.core.common.delegate

import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewbinding.ViewBinding
import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// =============================================================================================
// =                                        Activity                                         =
// =============================================================================================

/**
 * 一个生命周期感知、线程安全的 Activity ViewBinding 委托。
 *
 * 它能确保 ViewBinding 实例被懒加载，并且在 Activity 销毁时自动清理，
 * 以防止在配置变更（如屏幕旋转）期间发生内存泄漏。
 *
 * ### 使用方法:
 * ```kotlin
 * class MyActivity : AppCompatActivity() {
 *     // 使用 viewBinding 函数进行属性委托
 *     private val binding by viewBinding(ActivityMyBinding::inflate)
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         // 在首次访问 binding 时，它会被懒加载创建
 *         setContentView(binding.root)
 *         binding.textView.text = "Hello from Activity!"
 *     }
 * }
 * ```
 * @param T ViewBinding 类的类型。
 * @param viewBinder 一个用于从 [LayoutInflater] 创建 ViewBinding 实例的 lambda。
 *                   这通常是生成的绑定类的静态 `inflate` 方法，例如 `ActivityMyBinding::inflate`。
 * @return 一个管理 ViewBinding 实例的 [ReadOnlyProperty] 委托。
 */
@MainThread
fun <T : ViewBinding> ComponentActivity.viewBinding(
    viewBinder: (LayoutInflater) -> T
): ReadOnlyProperty<ComponentActivity, T> = ActivityViewBindingProperty(this, viewBinder)

private class ActivityViewBindingProperty<T : ViewBinding>(
    activity: ComponentActivity,
    private val viewBinder: (LayoutInflater) -> T
) : ReadOnlyProperty<ComponentActivity, T> {

    // 缓存的 ViewBinding 实例
    private var binding: T? = null

    init {
        // 注册一个生命周期观察者，在 Activity 销毁时清空 binding
        // 这在 init 块中完成，以确保对于每个委托实例，观察者只被添加一次
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                // 当 Activity 销毁时，清空 binding 引用以防止内存泄漏
                binding = null
                // 观察者会被 lifecycle 组件自动移除
            }
        })
    }

    @MainThread
    override fun getValue(thisRef: ComponentActivity, property: KProperty<*>): T {
        // 1. 如果 binding 已缓存，直接返回。这是最快、最常见的路径。
        binding?.let { return it }

        // 2. 强制主线程访问，这是保证 UI 组件线程安全最简单有效的策略。
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "ViewBinding 必须在主线程访问。当前线程: ${Thread.currentThread().name}"
        }

        // 3. 创建 binding 实例，缓存它，然后返回。
        return viewBinder(thisRef.layoutInflater).also { this.binding = it }
    }
}


// =============================================================================================
// =                                        Fragment                                         =
// =============================================================================================

/**
 * 一个线程安全、生命周期感知、无内存泄漏的 Fragment ViewBinding 委托。
 *
 * 通过 `private val binding by viewBinding(FragmentYourBinding::bind)` 在 Fragment 中使用。
 *
 * @param viewBinder 用于从 View 创建 ViewBinding 实例的 lambda (例如: FragmentYourBinding::bind)。
 */
@MainThread
fun <T : ViewBinding> Fragment.viewBinding(
    viewBinder: (View) -> T
): ReadOnlyProperty<Fragment, T> = FragmentViewBindingProperty(this, viewBinder)

private class FragmentViewBindingProperty<T : ViewBinding>(
    fragment: Fragment,
    private val viewBinder: (View) -> T
) : ReadOnlyProperty<Fragment, T> {

    // 缓存的 ViewBinding 实例
    private var binding: T? = null
    // 使用弱引用持有 Fragment，防止委托本身造成 Fragment 泄漏
    private val fragmentRef = WeakReference(fragment)

    // 这个 Observer 负责在视图生命周期拥有者销毁时，清空 binding
    private val viewLifecycleObserver = Observer<LifecycleOwner?> { owner ->
        if (owner == null) {
            binding = null
        }
    }

    init {
        // 使用一个只负责注册和反注册的“元观察者”，它监听 Fragment 本身的生命周期
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                // 当 Fragment 创建时，开始监听 viewLifecycleOwner 的变化
                fragmentRef.get()?.viewLifecycleOwnerLiveData?.observeForever(viewLifecycleObserver)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                // 当 Fragment 销毁时，停止监听，防止内存泄漏
                fragmentRef.get()?.viewLifecycleOwnerLiveData?.removeObserver(viewLifecycleObserver)
            }
        })
    }

    @MainThread
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        // 1. 优先返回已缓存的 binding
        binding?.let { return it }

        // 2. 强制主线程访问
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "ViewBinding 必须在主线程访问。当前线程: ${Thread.currentThread().name}"
        }

        // 3. 获取 Fragment 实例，如果已被 GC 则抛出异常
        val fragment = fragmentRef.get()
        checkNotNull(fragment) { "Fragment 实例已被销毁。" }

        // 4. 【核心检查】确保 Fragment 的 view 已经创建
        // 这可以防止在 onCreateView() 之前或 onDestroyView() 之后访问 binding 导致的崩溃
        val view = fragment.view
        checkNotNull(view) {
            "无法访问 ViewBinding，因为 Fragment 的 view 是 null。" +
                    "请确保你在 onCreateView() 之后和 onDestroyView() 之前访问 binding。"
        }

        // 5. 【双重保险】验证视图的生命周期处于有效状态
        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        check(lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            "无法在一个已销毁的视图生命周期状态下访问 ViewBinding。当前状态: ${lifecycle.currentState}"
        }

        // 6. 创建、缓存并返回 binding
        return viewBinder(view).also { this.binding = it }
    }
}