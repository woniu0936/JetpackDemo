package com.demo.jetpack.common.dataflow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * 初始数据加载期间发生的业务异常的基类。
 *
 * 作为一个 `sealed class`，它确保所有可能的初始加载失败类型都在此文件中定义，
 * 从而可以在 `when` 表达式中进行详尽的处理。
 *
 * 它继承自 `RuntimeException`，以符合 Kotlin 协程的错误处理最佳实践。
 * 这允许调用者使用 `Flow.catch` 操作符或 `runCatching` 优雅地、选择性地处理这些预期的业务异常，
 * 而无需强制使用 `try-catch` 块。
 *
 * @property requestId 当前数据请求的唯一 ID，用于日志记录和追踪。
 */
sealed class InitialDataLoadException(
    val requestId: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * 当设备离线**且**没有可用的本地缓存作为回退时抛出。
 * 这代表了由环境因素（网络）引起的典型瞬时故障，使其可重试。
 *
 * --- 业务场景 ---
 * 在以下情况抛出:
 * 1. `local()` 返回 `null`（或抛出异常）。
 * 并且
 * 2. `isOnline()` 返回 `false`。
 */
class NetworkUnavailableException(
    requestId: String,
) : InitialDataLoadException(
    requestId = requestId,
    message = "Network is unavailable and no cached data is available [req=$requestId]",
)

/**
 * 当远程获取成功但返回空结果，**且**没有可用的本地缓存时抛出。
 * 这不是技术故障，而是业务层面的“未找到”状态（例如，查询不存在的用户 ID）。
 * 这种情况通常不可重试，因为数据源本身就缺少数据。
 *
 * --- 业务场景 ---
 * 在以下情况抛出:
 * 1. `local()` 返回 `null`（或抛出异常）。
 * 并且
 * 2. `isOnline()` 返回 `true`。
 * 并且
 * 3. `remote()` 成功执行但返回 `null`。
 */
class RemoteEmptyException(
    requestId: String,
) : InitialDataLoadException(
    requestId = requestId,
    message = "Remote source returned empty data and no cache is available [req=$requestId]",
)

/**
 * 当远程数据获取因技术原因（例如，服务器 5xx 错误、网络超时、DNS 问题）失败，
 * **且**没有可用的本地缓存时抛出。
 * 它表示与远程服务器通信期间的技术故障，通常是瞬态且可重试的。
 *
 * --- 业务场景 ---
 * 在以下情况抛出:
 * 1. `local()` 返回 `null`（或抛出异常）。
 * 并且
 * 2. `isOnline()` 返回 `true`。
 * 并且
 * 3. `remote()` 抛出异常（例如，IOException, HttpException）。
 */
class RemoteFailedException(
    requestId: String,
    cause: Throwable,
) : InitialDataLoadException(
    requestId = requestId,
    message = "Remote request failed and no cache is available [req=$requestId][cause=${cause.javaClass.simpleName}]",
    cause = cause, // Wrap the original exception for debugging purposes
)

/**
 * 一个专门用于处理 `InitialDataLoadException` 的 Flow `catch` 操作符。
 * 它提供了一个类型安全的 DSL，用于为不同类型的加载失败定义处理逻辑。
 *
 * 使用示例：
 * repository.getData()
 *     .onStart { _uiState.value = UiState.Loading }
 *     .catchInitialError(
 *         // 只需为关心的错误类型提供处理逻辑，代码意图非常清晰
 *         onNetworkUnavailable = { _uiState.value = UiState.Error("请检查网络连接") },
 *         onRemoteEmpty = { _uiState.value = UiState.Empty("未找到相关数据") },
 *         onRemoteFailed = { _uiState.value = UiState.Error("加载失败，请重试") },
 *         onUnknown = { _uiState.value = UiState.Error("发生未知错误") }
 *     )
 *     .collect { data ->
 *         _uiState.value = UiState.Success(data)
 *     }
 *
 * @param onNetworkUnavailable 当发生 `NetworkUnavailableException` 时调用的 lambda。
 * @param onRemoteEmpty 当发生 `RemoteEmptyException` 时调用的 lambda。
 * @param onRemoteFailed 当发生 `RemoteFailedException` 时调用的 lambda。
 * @param onUnknown 当发生任何其他类型的 `Throwable` 时调用的 lambda。
 * @return 返回一个新的 Flow，它附加了错误处理逻辑。
 */
inline fun <T> Flow<T>.catchInitialError(
    crossinline onNetworkUnavailable: (NetworkUnavailableException) -> Unit = {},
    crossinline onRemoteEmpty: (RemoteEmptyException) -> Unit = {},
    crossinline onRemoteFailed: (RemoteFailedException) -> Unit = {},
    crossinline onUnknown: (Throwable) -> Unit = {}
): Flow<T> {
    // 使用标准的 .catch 操作符作为基础
    return this.catch { error ->
        // 在 catch 块内部，使用 when 表达式进行类型安全的检查
        when (error) {
            is NetworkUnavailableException -> onNetworkUnavailable(error)
            is RemoteEmptyException -> onRemoteEmpty(error)
            is RemoteFailedException -> onRemoteFailed(error)
            else -> onUnknown(error)
        }
    }
}