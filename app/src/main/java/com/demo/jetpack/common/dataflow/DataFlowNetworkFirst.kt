@file:OptIn(ExperimentalCoroutinesApi::class)

package com.demo.jetpack.common.dataflow

import com.novel.library.extensions.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform

// --- Internal Core Logic (With Bug Fix) ---

/**
 * [Internal Core Implementation]
 * The core logic, now fully reactive to network status changes.
 * It MUST receive the online status as a Flow.
 */
@PublishedApi
internal inline fun <T : Any, R : Any> dataFlowNetworkFirstInternal(
    isOnline: Flow<Boolean>,
    crossinline fetch: DataSource<R>,
    crossinline query: DataSource<T>,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = isOnline
    .distinctUntilChanged()
    .flatMapLatest { online ->
        channelFlow<DataState<T>> {
            logD("dataFlowNetworkFirst") { "Network state is ${if (online) "ONLINE" else "OFFLINE"}. Starting logic." }
            send(DataState.Loading)

            // --- FIX: Defined as a local suspend function instead of a lambda variable ---
            // This is the cleanest and most robust way to create reusable code
            // that needs access to the parent coroutine scope (for `send`).
            suspend fun collectFromCache() {
                logD("dataFlowNetworkFirst") { "Collecting from local source (cache)." }
                query().distinctUntilChanged().collect { data ->
                    send(if (data != null) DataState.Success(data) else DataState.Empty)
                }
            }

            if (online) {
                try {
                    val networkData = fetch().firstOrNull()
                    if (networkData != null) {
                        logD("dataFlowNetworkFirst") { "FETCH successful. Saving data." }
                        save(networkData)
                        collectFromCache() // Now this call is clean and works correctly.
                    } else {
                        logD("dataFlowNetworkFirst") { "FETCH successful but returned no data. Falling back to cache." }
                        collectFromCache()
                    }
                } catch (networkError: Throwable) {
                    logD("dataFlowNetworkFirst") { "FETCH failed with error. Emitting error and falling back to cache." }
                    send(DataState.Error(networkError))
                    collectFromCache()
                }
            } else {
                logD("dataFlowNetworkFirst") { "Device is OFFLINE. Collecting from cache immediately." }
                collectFromCache()
            }
        }
    }.catch { e ->
        logD("dataFlowNetworkFirst") { "An unexpected error occurred in the flow: ${e.message}" }
        emit(DataState.Error(e))
    }
    .suppressErrorOnSuccess()
    .flowOn(Dispatchers.IO)


// --- Public APIs (Unchanged, as they were correct) ---

// --- Group 1: `isOnline` is a `suspend () -> Boolean` (One-time check) ---

/**
 * [1/8] isOnline: suspend, query: Flow, fetch: suspend
 */
inline fun <T : Any, R : Any> dataFlowNetworkFirst(
    crossinline isOnline: suspend () -> Boolean,
    crossinline fetch: suspend () -> R,
    crossinline query: () -> Flow<T?>,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = dataFlowNetworkFirstInternal(
    isOnline = flow { emit(isOnline()) }, // Adapt suspend to Flow for one-time check
    fetch = fromSuspend { fetch() },
    query = query,
    save = save
)

/**
 * [2/8] isOnline: suspend, query: suspend, fetch: suspend
 */
inline fun <T : Any, R : Any> dataFlowNetworkFirst(
    crossinline isOnline: suspend () -> Boolean,
    crossinline fetch: suspend () -> R,
    crossinline query: suspend () -> T?,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = dataFlowNetworkFirstInternal(
    isOnline = flow { emit(isOnline()) },
    fetch = fromSuspend { fetch() },
    query = fromSuspend { query() },
    save = save
)

/**
 * [3/8] isOnline: suspend, query: Flow, fetch: Flow
 */
inline fun <T : Any, R : Any> dataFlowNetworkFirst(
    crossinline isOnline: suspend () -> Boolean,
    crossinline fetch: () -> Flow<R?>,
    crossinline query: () -> Flow<T?>,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = dataFlowNetworkFirstInternal(
    isOnline = flow { emit(isOnline()) },
    fetch = fetch,
    query = query,
    save = save
)

/**
 * [4/8] isOnline: suspend, query: suspend, fetch: Flow
 */
inline fun <T : Any, R : Any> dataFlowNetworkFirst(
    crossinline isOnline: suspend () -> Boolean,
    crossinline fetch: () -> Flow<R?>,
    crossinline query: suspend () -> T?,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = dataFlowNetworkFirstInternal(
    isOnline = flow { emit(isOnline()) },
    fetch = fetch,
    query = fromSuspend { query() },
    save = save
)

// --- Group 2: `isOnline` is a `Flow<Boolean>` (Reactive) ---

/**
 * [5/8] isOnline: Flow, query: Flow, fetch: suspend
 */
inline fun <T : Any, R : Any> dataFlowNetworkFirst(
    isOnline: Flow<Boolean>,
    crossinline fetch: suspend () -> R,
    crossinline query: () -> Flow<T?>,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = dataFlowNetworkFirstInternal(
    isOnline = isOnline, // Pass Flow directly
    fetch = fromSuspend { fetch() },
    query = query,
    save = save
)

/**
 * [6/8] isOnline: Flow, query: suspend, fetch: suspend
 */
inline fun <T : Any, R : Any> dataFlowNetworkFirst(
    isOnline: Flow<Boolean>,
    crossinline fetch: suspend () -> R,
    crossinline query: suspend () -> T?,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = dataFlowNetworkFirstInternal(
    isOnline = isOnline,
    fetch = fromSuspend { fetch() },
    query = fromSuspend { query() },
    save = save
)

/**
 * [7/8] isOnline: Flow, query: Flow, fetch: Flow
 */
inline fun <T : Any, R : Any> dataFlowNetworkFirst(
    isOnline: Flow<Boolean>,
    crossinline fetch: () -> Flow<R?>,
    crossinline query: () -> Flow<T?>,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = dataFlowNetworkFirstInternal(
    isOnline = isOnline,
    fetch = fetch,
    query = query,
    save = save
)

/**
 * [8/8] isOnline: Flow, query: suspend, fetch: Flow
 */
inline fun <T : Any, R : Any> dataFlowNetworkFirst(
    isOnline: Flow<Boolean>,
    crossinline fetch: () -> Flow<R?>,
    crossinline query: suspend () -> T?,
    crossinline save: suspend (R) -> Unit
): Flow<DataState<T>> = dataFlowNetworkFirstInternal(
    isOnline = isOnline,
    fetch = fetch,
    query = fromSuspend { query() },
    save = save
)

/**
 * Loading → Error → Empty/Success(from cache) to Loading → Empty/Success(from cache)
 * A Flow operator that implements a specific UI/UX policy:
 * It suppresses an intermediate `Error` state if a `Success` or `Empty` state follows it.
 *
 * This implementation is thread-safe and conforms to the Flow API specification.
 * It uses `transform` to handle element-by-element logic and `onCompletion`
 * to handle the final state when the flow completes.
 *
 * @return A new Flow that filters out temporary errors.
 */
fun <T> Flow<DataState<T>>.suppressErrorOnSuccess(): Flow<DataState<T>> {
    // This state is safely confined within the operator for a single collector.
    // Flow guarantees that `transform` and `onCompletion` are called sequentially
    // for a single collection, so this is not a race condition.
    var pendingError: DataState.Error? = null

    return this
        .transform { value ->
            when (value) {
                is DataState.Loading -> {
                    // A new loading sequence starts, so any previous pending error is irrelevant.
                    pendingError = null
                    emit(value)
                }

                is DataState.Error -> {
                    // An error occurred. Don't emit it yet, just hold it.
                    pendingError = value
                }

                is DataState.Success, is DataState.Empty -> {
                    // A definitive result has arrived. The pending error is now suppressed.
                    pendingError = null
                    emit(value) // Emit the successful result.
                }
            }
        }
        .onCompletion { cause ->
            // This block executes when the upstream flow is complete.
            // `cause` is null for normal completion, or an exception for abnormal completion.

            // If the flow completed normally AND we are left with a pending error,
            // it means the error was the very last significant event. Now we must emit it.
            if (cause == null && pendingError != null) {
                emit(pendingError!!)
            }
            // If the flow completed with an exception (cause != null), we let the exception
            // propagate downstream. The pendingError is irrelevant in this case.
        }
}
