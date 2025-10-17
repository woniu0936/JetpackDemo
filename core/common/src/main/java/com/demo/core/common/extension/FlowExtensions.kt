package com.demo.core.common.extension

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow
import kotlin.random.Random

/**
 * 【生产环境强化版】
 * 使用 **线性退避** 策略重试失败的 Flow。
 * 延迟时间随尝试次数线性增长。
 *
 * **适用场景**: 重试本地操作，或非关键、负载不高的网络请求。
 *
 * 延迟计算: `(attempt + 1) * initialDelay`
 *
 * @param T Flow 元素的类型。
 * @param retries 重试次数。默认为 3。
 * @param initialDelay 初始延迟毫秒数。默认为 1000ms。
 * @param shouldRetry (可选) 一个断言，根据异常类型决定是否继续重试。
 * @return 包含了线性退避重试逻辑的新 Flow。
 *
 * @example
 * flow { emit(myApi.fetchData()) }
 *     .retryWithLinearBackoff(retries = 5) { cause ->
 *         // 只在网络IO异常时重试
 *         cause is java.io.IOException
 *     }
 */
fun <T> Flow<T>.retryWithLinearBackoff(
    retries: Int = 3,
    initialDelay: Long = 1000,
    shouldRetry: (cause: Throwable) -> Boolean = { true }
): Flow<T> {
    require(retries >= 0) { "Retries must be non-negative" }
    require(initialDelay >= 0) { "Initial delay must be non-negative" }

    return retryWithBackoff(retries, shouldRetry) { attempt ->
        (attempt + 1) * initialDelay
    }
}

/**
 * 【生产环境强化版】
 * 使用 **指数退避 + 抖动 (Jitter)** 策略重试失败的 Flow。
 * 这是处理网络请求重试的**行业标准**。
 *
 * **适用场景**: 绝大多数网络操作，特别是与可能过载或不稳定的外部服务交互时。
 *
 * 延迟计算: `(initialDelay * (factor ^ attempt)) * (1 ± jitter)`
 *
 * @param T Flow 元素的类型。
 * @param retries 重试次数。默认为 3。
 * @param initialDelay 基础延迟毫秒数。默认为 1000ms。
 * @param factor 每次重试延迟的乘数因子，必须 >= 1.0。默认为 2.0。
 * @param maxDelay 允许的最大延迟时间，防止延迟无限增长。默认为 1 分钟。
 * @param jitter 延迟时间的随机抖动因子，用于防止“重试风暴”。例如 0.1 表示 ±10%。默认为 0.0 (不抖动)。
 * @param shouldRetry (可选) 一个断言，根据异常类型决定是否继续重试。
 * @return 包含了指数退避重试逻辑的新 Flow。
 *
 * @example
 * flow { emit(myApi.criticalOperation()) }
 *     .retryWithExponentialBackoff(
 *         retries = 5,
 *         initialDelay = 500,
 *         factor = 2.0,
 *         jitter = 0.1
 *     ) { cause ->
 *         // 只在可恢复的 HttpException (如 502, 503, 504) 时重试
 *         cause is retrofit2.HttpException && cause.code() >= 500
 *     }
 */
fun <T> Flow<T>.retryWithExponentialBackoff(
    retries: Int = 3,
    initialDelay: Long = 1000,
    factor: Double = 2.0,
    maxDelay: Long = 60_000,
    jitter: Double = 0.0,
    shouldRetry: (cause: Throwable) -> Boolean = { true }
): Flow<T> {
    require(retries >= 0) { "Retries must be non-negative" }
    require(initialDelay > 0) { "Initial delay must be positive" }
    require(factor >= 1.0) { "Factor must be >= 1.0" }
    require(maxDelay >= initialDelay) { "Max delay must be >= initial delay" }
    require(jitter in 0.0..1.0) { "Jitter must be in the range [0.0, 1.0]" }

    return retryWithBackoff(retries, shouldRetry) { attempt ->
        // 【健壮性强化】计算延迟，并增加溢出保护
        val delayTime = (initialDelay.toDouble() * factor.pow(attempt.toInt()))
            .toLong()
            .coerceAtLeast(0L) // ← 先保底 0，防止溢出成负数
            .coerceAtMost(maxDelay) // ← 再封顶

        val jitteredDelay = if (jitter > 0.0) {
            val randomFactor = Random.nextDouble(1.0 - jitter, 1.0 + jitter)
            (delayTime * randomFactor).toLong()
        } else {
            delayTime
        }
        jitteredDelay.coerceAtMost(maxDelay)
    }
}

/**
 * 【最终优化版】
 * 使用一个通用的、可配置的退避策略来重试失败的 Flow。
 * 这是一个私有的辅助函数，统一了所有重试逻辑。
 *
 * @param retries 总重试次数。
 * @param shouldRetry 一个断言，根据异常类型决定是否应该重试。
 * @param delayStrategy 一个 lambda，根据尝试次数计算下一次的延迟时间。
 */
private fun <T> Flow<T>.retryWithBackoff(
    retries: Int,
    shouldRetry: (cause: Throwable) -> Boolean = { true },
    delayStrategy: (attempt: Long) -> Long
): Flow<T> = retryWhen { cause, attempt ->
    if (shouldRetry(cause) && attempt < retries) {
        val delayTime = delayStrategy(attempt)
        delay(delayTime)
        true
    } else {
        false
    }
}

/**
 * 【生产环境强化版】
 * 一个组合操作符，它先对 Flow 的每个元素执行 `map` 转换，然后使用 `distinctUntilChanged`
 * 过滤掉连续重复的转换结果。
 *
 * 这是一个非常常见的组合，例如，在UI层观察数据模型时，我们常常只关心其中某个字段的变化。
 *
 * **命名解析**: 明确使用 `mapAndDistinctUntilChanged` 是为了避免与 `distinct` (过滤所有重复) 产生混淆。
 *
 * @param T 上游 Flow 元素的类型。
 * @param R 转换后的元素类型。
 * @param transform 应用于每个元素的数据转换函数。
 * @return 一个包含了 `map` 和 `distinctUntilChanged` 逻辑的新 Flow。
 *
 * @example
 * // 假设 userFlow 会发射多个 name 相同的 User 对象
 * // UI 层只会在 user.id 发生变化时才收到更新
 * userFlow.mapAndDistinctUntilChanged { user -> user.id }
 *     .onEach { userId ->
 *         // ... update UI with the new user ID ...
 *     }
 *     .launchIn(viewModelScope)
 */
inline fun <T, R> Flow<T>.mapAndDistinctUntilChanged(
    crossinline transform: (T) -> R
): Flow<R> = this
    .map(transform)
    .distinctUntilChanged()

/**
 * 【生产环境强化版】
 * 对 Flow 的前 `count` 个元素执行指定的 `action`。
 *
 * 这是一个用于执行“有限次数副作用”的操作符，类似于 `onEach`，但只执行指定的次数。
 *
 * **命名解析**: `onEachCounted` 清晰地表明了这是一个基于计数的 `onEach` 变体。
 *
 * @param T Flow 元素的类型。
 * @param count 要执行 `action` 的最大次数。必须大于 0。
 * @param action 对前 `count` 个元素执行的挂起函数。
 * @return 返回原始 Flow，行为不受影响。
 *
 * @example
 * // 假设我们只想对首次加载成功的数据进行一次分析上报
 * dataFlow
 *     .onEachCounted(1) { firstData ->
 *         Analytics.track("first_data_loaded", mapOf("size" to firstData.size))
 *     }
 *     .onEach { allData ->
 *         // ... 正常处理所有数据 ...
 *     }
 *     .launchIn(scope)
 */
inline fun <T> Flow<T>.onEachCounted(
    count: Int = 1,
    crossinline action: suspend (value: T) -> Unit
): Flow<T> {
    // 【健壮性】在入口处进行参数校验
    require(count > 0) { "Count must be positive." }

    // 使用 AtomicInteger 保证在并发场景下的线程安全，虽然 onEach 是顺序执行的，
    // 但这是一个更健壮的实践。
    val invocations = AtomicInteger(0)

    // 【性能优化】使用 onEach 比 transform 更轻量，因为它不创建新的 Flow 上下文
    return this.onEach { value ->
        // 使用 getAndIncrement 原子地获取并增加计数
        if (invocations.getAndIncrement() < count) {
            action(value)
        }
    }
}