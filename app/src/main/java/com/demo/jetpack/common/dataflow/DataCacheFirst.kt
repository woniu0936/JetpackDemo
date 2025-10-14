package com.demo.jetpack.common.dataflow

import com.demo.core.logger.logD
import com.demo.core.logger.logE
import com.demo.core.logger.logW
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * 一个实现了“缓存优先”数据加载策略的内部核心函数。
 *
 * @param T 要获取的数据类型。
 * @param isOnline 检查当前网络状态的 lambda 函数。
 * @param local 从本地缓存（例如，数据库）获取数据的 suspend 函数。
 * @param remote 从远程源（例如，API）获取数据的 suspend 函数。
 * @param cacheRemote 将获取到的远程数据保存到本地缓存的 suspend 函数。
 * @param shouldFetchRemote 一个谓词，用于决定即使本地数据存在是否需要从远程获取数据。默认为 `true`。
 * @param shouldEmitRemote 一个谓词，用于决定是否应将新的远程数据发送给收集器。默认为 `true`。
 *
 * @return 一个 `Flow`，它首先发出本地数据（如果可用），然后发出远程数据（如果已获取且允许）。
 * @throws InitialDataLoadException 在无法提供任何数据（无缓存且远程获取失败）的情况下抛出。
 */
@PublishedApi
internal inline fun <T : Any> dataCacheFirstInternal(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit,
    crossinline shouldFetchRemote: (T?) -> Boolean = { true },
    crossinline shouldEmitRemote: (localData: T?, remoteData: T) -> Boolean = { _, _ -> true }
): Flow<T> = channelFlow {
    // 为每个数据请求生成一个唯一的ID，用于在日志中追踪其生命周期，便于调试和问题定位。
    val requestId = LogTracer.newId()
    logD(LogTracer.TAG) { "[$requestId] >>>>> dataCacheFirstInternal start" }

    // 步骤 1: 尝试从本地缓存加载数据。
    // 使用 `runCatching` 捕获本地数据源（如数据库损坏）可能抛出的异常，
    // 防止其导致整个数据流中断，确保程序的健壮性。
    val localData = runCatching {
        LogTracer.trace(requestId) { local() }
    }.onFailure {
        // 记录本地读取失败的警告，但不会中断流程，因为可能还有远程数据可用。
        logW(LogTracer.TAG, it) { "[$requestId] local read failed" }
    }.getOrNull()
    logD(LogTracer.TAG) { "[$requestId] localData=$localData" }

    // 如果成功从本地获取到数据，立即将其发送给收集器。
    // 这提供了快速的UI响应，即使数据可能不是最新的。
    localData?.let {
        logD(LogTracer.TAG) { "[$requestId] emit local" }
        send(it)
    }

    // 步骤 2: 确定是否需要从远程源获取数据。
    // 如果本地没有数据，或者 `shouldFetchRemote` 策略（由调用者定义）指示需要刷新数据，
    // 则进行远程获取。这允许灵活地控制数据新鲜度。
    val shouldFetch = localData == null || shouldFetchRemote(localData)
    logD(LogTracer.TAG) { "[$requestId] shouldFetch=$shouldFetch" }
    if (!shouldFetch) {
        logD(LogTracer.TAG) { "[$requestId] skip remote" }
        // 如果不需要远程获取，则当前数据流完成，不再进行后续操作。
        return@channelFlow
    }

    // 步骤 3: 检查网络状态。
    // 只有在线时才尝试进行远程数据获取。
    if (!isOnline()) {
        // 记录离线状态，这通常不是错误，而是预期行为。
        logD(LogTracer.TAG) { "[$requestId] skip remote: offline" }
        if (localData == null) {
            // 这是一个关键的故障场景：如果离线且没有本地缓存数据可用，
            // 则无法提供任何数据，此时抛出特定异常通知调用者。
            throw NetworkUnavailableException(requestId)
        }
        // 如果有本地缓存作为备用，即使离线也安全完成流程，不中断用户体验。
        return@channelFlow
    }

    // 步骤 4: 执行远程数据获取。
    // 使用 `runCatching` 封装远程调用，以优雅地处理网络请求可能抛出的各种异常。
    runCatching {
        LogTracer.trace(requestId) { remote() }
    }.onSuccess { remoteData ->
        // 在处理远程数据之前，检查协程是否已被取消。如果已取消，则停止进一步处理，
        // 避免不必要的工作和潜在的资源泄漏。
        if (!isActive) {
            logD(LogTracer.TAG) { "[$requestId] remote success but channel closed" }
            return@onSuccess
        }
        logD(LogTracer.TAG) { "[$requestId] remoteData=$remoteData" }

        when {
            // 如果成功从远程源获取到数据。
            remoteData != null -> {
                // 在执行可能耗时的缓存操作之前和之后，检查协程的活跃状态，
                // 确保在协程被取消时能够及时响应，避免阻塞。
                coroutineContext.ensureActive()
                LogTracer.trace(requestId) { cacheRemote(remoteData) }
                coroutineContext.ensureActive()

                // 根据 `shouldEmitRemote` 策略决定是否将新获取的远程数据发送给收集器。
                // 这允许调用者控制UI何时更新。
                if (shouldEmitRemote(localData, remoteData)) {
                    logD(LogTracer.TAG) { "[$requestId] emit remote" }
                    send(remoteData)
                }
            }
            // 远程调用成功但返回 `null`，并且本地也没有缓存数据可供回退。
            localData == null -> {
                logE(LogTracer.TAG) { "[$requestId] remote null && local null -> throw" }
                // 抛出 `RemoteEmptyException`，表示远程源没有数据且无本地备用。
                throw RemoteEmptyException(requestId)
            }
            // 远程返回 `null`，但本地数据存在。在这种情况下，我们保留并继续使用过时的本地数据。
            else -> {
                logW(LogTracer.TAG) { "[$requestId] remote null, keep stale local" }
            }
        }
    }.onFailure { ex ->
        // 处理远程请求失败的情况（例如，网络错误、服务器错误）。
        if (localData == null) {
            // 这是一个关键的故障场景：如果远程请求失败且没有本地缓存数据可用，
            // 则无法提供任何数据，此时抛出特定异常通知调用者。
            logE(LogTracer.TAG, ex) { "[$requestId] remote failed && local null -> throw" }
            // 将原始异常包装在 `RemoteFailedException` 中，提供更丰富的错误信息。
            throw RemoteFailedException(requestId, ex)
        } else {
            // 如果有本地缓存作为备用，远程故障不应中断用户体验。
            // 仅记录警告，并继续使用已有的本地数据。
            logW(LogTracer.TAG, ex) { "[$requestId] remote failed, keep stale local" }
        }
    }

    logD(LogTracer.TAG) { "[$requestId] <<<<< dataCacheFirstInternal end" }
}.flowOn(Dispatchers.IO) // 确保所有数据操作都在 IO 调度器上执行，避免阻塞主线程。

/**
 * 一个健壮的、响应式的数据加载函数，实现了“缓存优先”策略。
 * 此函数返回一个长生命周期的 `Flow<T>`，非常适合 UI 需要随时间对数据变化做出反应的场景。
 *
 * ### 核心行为:
 * 1.  **缓存优先 (Cache First)**: 如果本地数据源存在数据，则立即发射。
 * 2.  **响应式更新 (Reactive Updates)**: 持续监听本地数据源 (`local` Flow)，并发射任何后续的变更。
 * 3.  **智能远程抓取 (Smart Remote Fetch)**: 基于 `shouldFetchRemote` 策略，智能地决定是否需要从远程源抓取新数据。
 * 4.  **单一数据源 (Single Source of Truth)**: 远程数据总是被保存到本地数据源 (`cacheRemote`)，然后通过响应式的 `local` Flow 触发一次发射，这确保了数据的一致性。
 * 5.  **离线适应性 (Offline Resilience)**: 如果远程抓取失败但有可用的缓存数据，Flow 会优雅地继续使用过时的数据。
 * 6.  **显式失败 (Explicit Failure)**: 仅在关键失败场景下（例如，离线且无缓存），Flow 才会以一个特定的 `InitialDataLoadException` 终止，从而允许下游进行清晰、可编程的错误处理。
 * 7.  **并发与线程 (Concurrency & Threading)**: 所有操作都在 `Dispatchers.IO` 上安全执行。
 * 8.  **防止泄漏 (Leak-Safe)**: 当下游收集器被取消时，所有后台作业都会通过 `awaitClose` 的机制被自动取消，从而保证了资源安全。
 *
 * @param T 数据的类型，被约束为不可空 (`Any`)。
 * @param isOnline 一个同步函数，用于检查当前的网络连接状态。
 * @param local 一个返回 `Flow<T?>` 的函数，从本地数据源（如 Room、DataStore）获取数据。
 * @param remote 一个挂起函数，用于从远程数据源（如 API）抓取最新数据。
 * @param cacheRemote 一个挂起函数，用于将远程抓取的数据保存到本地数据源。
 * @param shouldFetchRemote 一个策略函数，用于决定是否有必要进行远程抓取。
 * @return 一个 `Flow<T>`，它会发射可用的数据，并保持活动状态以监听未来的更新。
 */
@PublishedApi
internal inline fun <T : Any> dataCacheFirstFlowInternal(
    crossinline isOnline: () -> Boolean,
    crossinline local: () -> Flow<T?>,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit,
    crossinline shouldFetchRemote: (localData: T?) -> Boolean = { true }
): Flow<T> = channelFlow {
    // 为每个数据请求生成一个唯一的ID，用于在日志中追踪其生命周期，便于调试和问题定位。
    val requestId = LogTracer.newId()
    logD(LogTracer.TAG) { "[$requestId] >>>>> dataCacheFirstReactive started" }

    // --- 阶段 1: 发射初始缓存数据 ---
    // 尝试从本地数据源获取第一个可用的数据项。如果本地源是空的，则 `firstOrNull()` 返回 `null`。
    val initialCache = runCatching { local().firstOrNull() }.getOrNull()
    initialCache?.let {
        // 如果存在初始缓存数据，立即将其发送给收集器，以提供快速的UI响应。
        logD(LogTracer.TAG) { "[$requestId] Phase 1: Emitting initial cache. [data=$it]" }
        send(it)
    }

    // --- 阶段 2: 启动持久化本地观察者 ---
    // 启动一个独立的协程来持续监听本地数据源的变化。
    // `distinctUntilChanged()` 确保只有当数据真正发生变化时才触发更新，避免重复发射。
    // `filterNotNull()` 过滤掉 `null` 值，确保只处理有效数据。
    val observerJob = launch {
        logD(LogTracer.TAG) { "[$requestId] Phase 2: Starting persistent local observer job." }
        local()
            .distinctUntilChanged()
            .filterNotNull()
            .collect { localData ->
                // 当本地数据源更新时，将最新数据发送给收集器。
                logD(LogTracer.TAG) { "[$requestId] Local observer detected update. Emitting. [data=$localData]" }
                send(localData)
            }
    }

    // --- 阶段 3: 智能远程同步逻辑 ---
    // 根据是否存在初始缓存数据以及 `shouldFetchRemote` 策略，决定是否需要从远程同步数据。
    val shouldFetch = initialCache == null || shouldFetchRemote(initialCache)
    logD(LogTracer.TAG) { "[$requestId] Phase 3: Remote sync decision. [shouldFetch=$shouldFetch, hasInitialCache=${initialCache != null}]" }

    if (shouldFetch) {
        when {
            // 如果设备离线，则跳过远程同步。
            !isOnline() -> {
                logW(LogTracer.TAG) { "[$requestId] Remote sync skipped: Offline." }
                if (initialCache == null) {
                    // 如果离线且没有初始缓存数据，则这是一个关键故障，终止 Flow 并抛出网络不可用异常。
                    val offlineError = NetworkUnavailableException(requestId)
                    logE(LogTracer.TAG, offlineError) { "[$requestId] Critical failure: Offline with no cache. Closing Flow." }
                    close(offlineError) // 使用业务特定错误终止 Flow。
                }
            }

            // 如果在线，则尝试从远程获取数据。
            else -> {
                logD(LogTracer.TAG) { "[$requestId] Remote sync started: Online, attempting to fetch remote data..." }
                runCatching {
                    LogTracer.trace(requestId) { remote() }
                }.onSuccess { remoteData ->
                    logD(LogTracer.TAG) { "[$requestId] Remote fetch successful. [remoteData=$remoteData]" }
                    when {
                        // 如果远程数据不为空，则缓存数据并等待本地观察者发射更新。
                        remoteData != null -> {
                            logD(LogTracer.TAG) { "[$requestId] Caching remote data..." }
                            LogTracer.trace(requestId) { cacheRemote(remoteData) }
                            logD(LogTracer.TAG) { "[$requestId] Caching complete. Local observer will now emit the update." }
                        }

                        // 如果远程返回空且没有初始缓存，则这是一个关键故障，终止 Flow 并抛出远程空数据异常。
                        initialCache == null -> {
                            val emptyError = RemoteEmptyException(requestId)
                            logE(LogTracer.TAG, emptyError) { "[$requestId] Critical failure: Remote returned null with no cache. Closing Flow." }
                            close(emptyError)
                        }

                        // 如果远程返回空但有初始缓存，则保留旧缓存数据。
                        else -> {
                            logW(LogTracer.TAG) { "[$requestId] Remote returned null, but keeping existing stale cache." }
                        }
                    }
                }.onFailure { ex ->
                    when {
                        // 如果远程获取失败且没有初始缓存，并且不是取消异常，则这是一个关键故障，终止 Flow 并抛出远程失败异常。
                        initialCache == null && ex !is CancellationException -> {
                            val remoteError = RemoteFailedException(requestId, ex)
                            logE(LogTracer.TAG, remoteError) { "[$requestId] Critical failure: Remote fetch failed with no cache. Closing Flow." }
                            close(remoteError)
                        }

                        // 非关键故障：有缓存数据可用，吞下异常并继续使用旧缓存数据。
                        else -> {
                            logW(LogTracer.TAG, ex) { "[$requestId] Remote fetch failed, but proceeding with stale cache. [error=${ex.message}]" }
                        }
                    }
                }
            }
        }
    } else {
        // 如果 `shouldFetchRemote` 策略决定不进行远程同步，则跳过此阶段。
        logD(LogTracer.TAG) { "[$requestId] Phase 3: Remote sync skipped by 'shouldFetchRemote' policy." }
    }

    logD(LogTracer.TAG) { "[$requestId] Main setup complete. Flow is now active and awaiting updates." }

    // --- 阶段 4: 取消时清理资源 ---
    // `awaitClose` 块确保当 Flow 的收集器被取消或 Flow 完成时，
    // 相关的资源（如 `observerJob`）能够被正确地清理，防止内存泄漏。
    awaitClose {
        logD(LogTracer.TAG) { "[$requestId] <<<<< Flow was cancelled or completed. Cleaning up resources..." }
        observerJob.cancel()
        logD(LogTracer.TAG) { "[$requestId] Observer job cancelled. Cleanup finished." }
    }
}.flowOn(Dispatchers.IO)

inline fun <T : Any> dataCacheFirst(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit = {},
    crossinline shouldFetchRemote: (localData: T?) -> Boolean = { true },
    crossinline shouldEmitRemote: (localData: T?, remoteData: T) -> Boolean = { _, _ -> true }
): Flow<T> = dataCacheFirstInternal(isOnline, local, remote, cacheRemote, shouldFetchRemote, shouldEmitRemote)

internal inline fun <T : Any> dataCacheFirstFlow(
    crossinline isOnline: () -> Boolean,
    crossinline local: () -> Flow<T?>,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit = {},
    crossinline shouldFetchRemote: (localData: T?) -> Boolean = { true }
): Flow<T> = dataCacheFirstFlowInternal(isOnline, local, remote, cacheRemote, shouldFetchRemote)