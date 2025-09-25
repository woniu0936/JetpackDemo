package com.demo.jetpack.common.dataflow

import com.demo.jetpack.core.extension.logD
import com.demo.jetpack.core.extension.logE
import com.demo.jetpack.core.extension.logW
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * 实现“网络优先”数据加载策略。
 *
 * 它优先从远程源获取数据。如果远程获取成功，其数据将被发射并缓存。
 * 如果设备离线，或者远程获取失败，它将回退到本地数据源。
 *
 * ### 返回值语义:
 * 一个发射单个项目然后完成的 `Flow`:
 * - 发射 `T`: 如果数据成功地从远程或本地源检索到。
 * - 发射 `null`: **仅当**远程和本地源都被成功查询但没有返回数据，
 *   或者 `shouldEmitLocal` 策略返回 `false` 时。
 *   这表示“未找到数据”，而不是失败。
 * - 抛出 `InitialDataLoadException`: 仅在关键故障场景中，
 *   由于底层错误（例如，远程和本地都失败）而无法检索数据时。
 */
@PublishedApi
internal inline fun <T : Any> dataNetworkFirstInternal(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit,
    crossinline shouldEmitLocal: (localData: T) -> Boolean = { true },
): Flow<T?> = flow {

    // 为每个数据请求生成一个唯一的ID，用于在日志中追踪其生命周期，便于调试和问题定位。
    val requestId = LogTracer.newId()
    logD(LogTracer.TAG) { "[$requestId] >>>>> dataNetworkFirstInternal start" }

    // 检查当前网络状态。如果离线，则直接回退到本地数据源。
    if (!isOnline()) {
        logD(LogTracer.TAG) { "[$requestId] Offline, falling back to local source." }
        // 尝试从本地获取数据，并处理可能发生的异常。
        val localResult = runCatching { local() }
        localResult.onFailure {
            // 记录本地读取失败的警告。
            logW(LogTracer.TAG, it) { "[$requestId] Offline, local read failed." }
        }
        // 发射本地数据（如果存在），否则发射 null。
        emit(localResult.getOrNull())
    } else {
        logD(LogTracer.TAG) { "[$requestId] Online, attempting to fetch from remote source." }
        // 尝试从远程源获取数据，并使用 `runCatching` 捕获潜在异常。
        val remoteResult = runCatching { remote() }

        remoteResult.fold(
            onSuccess = { remoteData ->
                // 确保协程在处理结果时仍然活跃。
                coroutineContext.ensureActive()
                if (remoteData != null) {
                    logD(LogTracer.TAG) { "[$requestId] Remote fetch successful, emitting and caching." }
                    // 如果远程数据不为空，则发射数据并尝试缓存。
                    emit(remoteData)
                    runCatching { cacheRemote(remoteData) }
                        .onFailure { e -> logE(LogTracer.TAG, e) { "[$requestId] cacheRemote failed" } }
                } else {
                    logW(LogTracer.TAG) { "[$requestId] Remote returned null, falling back to local." }
                    // 如果远程返回 null，则回退到本地数据源。
                    val localResult = runCatching { local() }
                    val localData = localResult.getOrNull()
                    // 如果本地数据存在且 `shouldEmitLocal` 策略允许，则发射本地数据，否则发射 null。
                    if (localData != null && shouldEmitLocal(localData)) {
                        emit(localData)
                    } else {
                        emit(null)
                    }
                }
            },
            onFailure = { remoteException ->
                // 如果远程请求被取消，则重新抛出 `CancellationException`。
                if (remoteException is CancellationException) throw remoteException
                // 确保协程在处理异常时仍然活跃。
                coroutineContext.ensureActive()

                logW(LogTracer.TAG, remoteException) { "[$requestId] Remote fetch failed, falling back to local." }
                // 远程获取失败，尝试从本地数据源获取数据作为回退。
                val localResult = runCatching { local() }
                val localData = localResult.getOrNull()

                when {
                    // 如果本地数据存在且 `shouldEmitLocal` 策略允许，则发射本地数据。
                    localData != null && shouldEmitLocal(localData) -> emit(localData)
                    // 如果本地数据获取成功但为空，则发射 null。
                    localResult.isSuccess -> emit(null)
                    // 如果本地数据获取也失败，则抛出 `RemoteFailedException`。
                    else -> throw RemoteFailedException(requestId, remoteException)
                }
            }
        )
    }

    logD(LogTracer.TAG) { "[$requestId] <<<<< dataNetworkFirstInternal end" }

}.flowOn(Dispatchers.IO) // 确保所有数据操作都在 IO 调度器上执行，避免阻塞主线程。

inline fun <T : Any> dataNetworkFirst(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit = {},
    crossinline shouldEmitLocal: (localData: T) -> Boolean = { true },
): Flow<T?> = dataNetworkFirstInternal(isOnline, local, remote, cacheRemote, shouldEmitLocal)