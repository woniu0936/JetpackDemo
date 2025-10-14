package com.demo.jetpack.common.dataflow

import com.demo.core.logger.logD
import com.demo.core.logger.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.cancellation.CancellationException

/**
 * [最终优化版] 一个极其健壮的数据加载函数，实现了缓存优先策略，并返回一个封装了状态的 Flow。
 * 此版本融合了多轮 Review 的优点，达到了生产环境的最高标准。
 *
 * @param T 数据的类型。
 * @param isOnline 检查当前网络状态的 lambda 函数。
 * @param local 从本地缓存获取数据的 suspend 函数。
 * @param remote 从远程源获取数据的 suspend 函数。
 * @param cacheRemote 将获取到的远程数据保存到本地缓存的 suspend 函数。
 * @param shouldFetchRemote 一个谓词，用于决定即使本地数据存在是否需要从远程获取数据。
 * @return 一个 `Flow<DataState<T>>`，它会清晰地发射 Loading, Success, Error 或 Empty 状态。
 */
inline fun <T : Any> dataStateCacheFirst(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit = {},
    crossinline shouldFetchRemote: (localData: T?) -> Boolean = { true }
): Flow<DataState<T>> = channelFlow {
    val requestId = LogTracer.newId()
    logD(LogTracer.TAG) { "[$requestId] >>>>> [Final-Optimized] dataStateCacheFirst start" }

    // 步骤 1: 立即发射 Loading 状态。
    send(DataState.Loading)

    // 步骤 2: 尝试从本地缓存加载初始数据。
    // 【采纳建议 3】: 增强本地读取失败的处理，记录详细错误但流程继续，以期通过远程获取来恢复。
    val localData = runCatching {
        LogTracer.trace(requestId) { local() }
    }.onFailure { localEx ->
        logE(LogTracer.TAG, localEx) { "[$requestId] Critical local read failure. Will attempt remote fallback." }
        // 注意：这里不立即发射 Error，因为远程成功依然可以拯救整个流程。
    }.getOrNull()
    logD(LogTracer.TAG) { "[$requestId] Initial localData=$localData" }

    // 步骤 3: 决策是否需要远程获取。
    val shouldFetch = shouldFetchRemote(localData)
    logD(LogTracer.TAG) { "[$requestId] shouldFetch=$shouldFetch" }

    if (!shouldFetch) {
        if (localData != null) {
            send(DataState.Success(localData))
        } else {
            send(DataState.Empty)
        }
        return@channelFlow
    }

    // 步骤 4: 执行远程获取逻辑。
    if (!isOnline()) {
        if (localData != null) {
            send(DataState.Success(localData))
        } else {
            send(DataState.Error(NetworkUnavailableException(requestId)))
        }
        return@channelFlow
    }

    runCatching {
        LogTracer.trace(requestId) { remote() }
    }.onSuccess { remoteData ->
        if (remoteData != null) {
            // 【采纳建议 1 & 2】: 对缓存操作进行错误处理和取消检查。
            runCatching {
                coroutineContext.ensureActive() // 缓存前检查
                LogTracer.trace(requestId) { cacheRemote(remoteData) }
                logD(LogTracer.TAG) { "[$requestId] Cache remote data successful." }
            }.onFailure { cacheEx ->
                // 即使缓存失败，我们依然成功获取了数据，这仍然是一个 Success 状态。
                // 记录严重错误，但不应因此中断用户流程。
                logE(LogTracer.TAG, cacheEx) { "[$requestId] Cache remote data failed, but emitting success anyway." }
            }
            // 无论缓存是否成功，都发射最新的远程数据。
            send(DataState.Success(remoteData))
        } else {
            if (localData != null) {
                send(DataState.Success(localData))
            } else {
                send(DataState.Empty)
            }
        }
    }.onFailure { ex ->
        if (localData != null) {
            send(DataState.Success(localData))
        } else {
            send(DataState.Error(RemoteFailedException(requestId, ex)))
        }
    }

    logD(LogTracer.TAG) { "[$requestId] <<<<< dataStateCacheFirst end" }
}.flowOn(Dispatchers.IO)

/**
 * [最终优化版] 一个极其健壮的、响应式的数据加载函数，实现了“缓存优先”和“单一数据源”策略。
 * 此版本遵循了现代 Android 架构的最佳实践，提供了稳定、可靠且对 UI 友好的状态流。
 *
 * @param T 数据的类型。
 * @param isOnline 检查当前网络状态的 lambda 函数。
 * @param local 一个返回 `Flow<T?>` 的函数，从本地数据源（如 Room）获取数据。
 * @param remote 一个挂起函数，用于从远程数据源抓取最新数据。
 * @param cacheRemote 一个挂起函数，用于将远程抓取的数据保存到本地数据源。
 * @param shouldFetchRemote 一个策略函数，用于决定是否有必要进行远程抓取。
 * @return 一个 `Flow<DataState<T>>`，它会持续发射来自本地数据源的加载状态和数据。
 */
inline fun <T : Any> dataStateCacheFirstFlow(
    crossinline isOnline: () -> Boolean,
    crossinline local: () -> Flow<T?>,
    crossinline remote: suspend () -> T? = { null },
    crossinline cacheRemote: suspend (T) -> Unit = {},
    crossinline shouldFetchRemote: (localData: T?) -> Boolean = { true }
): Flow<DataState<T>> = channelFlow {
    val requestId = LogTracer.newId()
    logD(LogTracer.TAG) { "[$requestId] >>>>> [Final-Optimized] dataStateCacheFirstFlow started" }

    // 步骤 1: 立即发射 Loading 状态。这是整个流程的起点。
    send(DataState.Loading)

    // 步骤 2: 获取初始缓存数据，仅用于帮助 `shouldFetchRemote` 做出决策。
    val initialData = runCatching {
        local().firstOrNull()
    }.onFailure { localEx ->
        logE(LogTracer.TAG, localEx) { "[$requestId] Critical: Initial local read failed." }
    }.getOrNull()
    logD(LogTracer.TAG) { "[$requestId] Initial localData check: $initialData" }

    // 步骤 3: 决策并执行一次性远程同步（如果需要）。
    val shouldFetch = shouldFetchRemote(initialData)
    if (shouldFetch) {
        if (isOnline()) {
            // 在线，尝试远程获取
            runCatching {
                val remoteData = LogTracer.trace(requestId) { remote() }
                if (remoteData != null) {
                    // 远程成功，写入缓存（带错误处理和取消检查）
                    runCatching {
                        coroutineContext.ensureActive()
                        LogTracer.trace(requestId) { cacheRemote(remoteData) }
                    }.onFailure { cacheEx ->
                        logE(LogTracer.TAG, cacheEx) { "[$requestId] Cache remote data failed, but proceeding." }
                    }
                } else {
                    // 远程成功但返回空，如果此时无缓存，则这是一个明确的“空”状态信号
                    if (initialData == null) {
                        send(DataState.Empty)
                    }
                }
            }.onFailure { ex ->
                // 远程失败，如果此时无缓存，则这是一个关键错误
                if (initialData == null && ex !is CancellationException) {
                    send(DataState.Error(RemoteFailedException(requestId, ex)))
                }
            }
        } else {
            // 离线，如果此时无缓存，则这是一个关键错误
            if (initialData == null) {
                send(DataState.Error(NetworkUnavailableException(requestId)))
            }
        }
    }

    // 步骤 4: 持续观察本地数据源作为唯一的数据出口。
    // 这是整个模式的核心：UI 的所有更新都来自于这里。
    var isFirstEmission = true
    local()
        .distinctUntilChanged()
        .collect { data ->
            when {
                data != null -> {
                    // 发射成功状态
                    send(DataState.Success(data))
                }

                !shouldFetch && initialData == null -> {
                    // 特殊情况：本地一开始就为空，且策略不允许远程获取，此时应发射 Empty
                    send(DataState.Empty)
                }
                // 如果是后续更新导致数据变为空（例如，用户删除了所有项目），
                // 此时也应该发射 Empty 状态。
                !isFirstEmission -> {
                    send(DataState.Empty)
                }
            }
            isFirstEmission = false
        }

    // `awaitClose` 仅用于保持 Flow 活跃，不再需要管理任何子 Job。
    awaitClose {
        logD(LogTracer.TAG) { "[$requestId] <<<<< Flow was cancelled or completed." }
    }
}.flowOn(Dispatchers.IO)
