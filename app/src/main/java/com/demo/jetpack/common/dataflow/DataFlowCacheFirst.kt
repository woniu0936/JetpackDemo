//package com.demo.jetpack.common.dataflow
//
//import com.novel.library.extensions.logD
//import com.novel.library.extensions.toJson
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.channelFlow
//import kotlinx.coroutines.flow.distinctUntilChanged
//import kotlinx.coroutines.flow.firstOrNull
//import kotlinx.coroutines.flow.flowOn
//
//// --- Internal Core Logic (Private) ---
//
///**
// * The internal, core implementation of the "cache-first" data flow strategy.
// * It orchestrates the process of loading, querying, fetching, and saving.
// * It relies on the `DataSource` abstraction, making it decoupled and stable.
// *
// * @param query The local data source (e.g., database), conforming to the DataSource interface.
// * @param fetch The remote data source (e.g., network), conforming to the DataSource interface.
// * @param saveFetchResult A suspend function to save the result from the fetch operation into the local source.
// * @param shouldFetch A predicate to decide if a remote fetch is necessary based on the cached data.
// */
//@PublishedApi
//internal inline fun <T : Any, R : Any> dataFlowCacheFirstInternal(
//    crossinline query: DataSource<T>,
//    crossinline fetch: DataSource<R>,
//    crossinline saveFetchResult: suspend (R?) -> Unit,
//    crossinline shouldFetch: (T?) -> Boolean
//): Flow<DataState<T>> = channelFlow {
//    logD("dataFlowCacheFirst") { "Process started. Emitting Loading state." }
//    send(DataState.Loading)
//
//    logD("dataFlowCacheFirst") { "Querying for initial cached data." }
//    val initialData = query().firstOrNull()
//    logD("dataFlowCacheFirst") { "Initial cached data is ${if (initialData == null) "NULL" else "PRESENT"}." }
//
//    val shouldFetchResult = shouldFetch(initialData)
//    logD("dataFlowCacheFirst") { "Decision from shouldFetch(): $shouldFetchResult" }
//
//    val fetchSuccess = if (shouldFetchResult) {
//        try {
//            logD("dataFlowCacheFirst") { "FETCHING new data from remote source." }
//            val fetchResult = fetch().firstOrNull()
//            logD("dataFlowCacheFirst") { "FETCH successful. Data received: ${fetchResult?.toJson()}" }
//
//            logD("dataFlowCacheFirst") { "SAVING fetched result to local source." }
//            saveFetchResult(fetchResult)
//            logD("dataFlowCacheFirst") { "SAVE successful." }
//            true
//        } catch (e: Throwable) {
//            logD("dataFlowCacheFirst") { "FETCH failed with error: ${e.message}" }
//            if (initialData == null) {
//                logD("dataFlowCacheFirst") { "Emitting Error state as there is no cached data." }
//                send(DataState.Error(e))
//            }
//            false
//        }
//    } else {
//        logD("dataFlowCacheFirst") { "Skipping fetch as per shouldFetch logic." }
//        true // Skipped fetch is considered a "success" in terms of flow control.
//    }
//
//    logD("dataFlowCacheFirst") { "Starting to collect from the single source of truth (local query)." }
//    query().distinctUntilChanged().collect { data ->
//        logD("dataFlowCacheFirst") { "Collected new data from local source: ${data?.toJson()}" }
//        if (data != null) {
//            logD("dataFlowCacheFirst") { "Emitting Success state." }
//            send(DataState.Success(data))
//        } else {
//            if (fetchSuccess) {
//                logD("dataFlowCacheFirst") { "Collected NULL from local source, but fetch was successful/skipped. Emitting Empty state." }
//                send(DataState.Empty)
//            }
//            // If fetch failed (fetchSuccess = false) and data is null, the Error state was already sent.
//        }
//    }
//}.flowOn(Dispatchers.IO)
//
//
//// --- Public API (User-Facing Functions) ---
//
///**
// * Case 1: The most common scenario.
// * Query from a local source that returns a Flow (e.g., Room).
// * Fetch from a remote source with a suspend function (e.g., Retrofit).
// */
//inline fun <T : Any, R : Any> dataFlowCacheFirst(
//    crossinline query: () -> Flow<T?>,
//    crossinline fetch: suspend () -> R?,
//    crossinline saveFetchResult: suspend (R?) -> Unit,
//    crossinline shouldFetch: (T?) -> Boolean = { it == null }
//): Flow<DataState<T>> = dataFlowCacheFirstInternal(
//    query = query, // Already matches DataSource type
//    fetch = fromSuspend { fetch() }, // Adapt suspend fun to DataSource
//    saveFetchResult = saveFetchResult,
//    shouldFetch = shouldFetch
//)
//
///**
// * Case 2: Both query and fetch operations are suspend functions.
// * Useful if the local cache is also accessed via a suspend function.
// */
//inline fun <T : Any, R : Any> dataFlowCacheFirst(
//    crossinline query: suspend () -> T?,
//    crossinline fetch: suspend () -> R?,
//    crossinline saveFetchResult: suspend (R?) -> Unit,
//    crossinline shouldFetch: (T?) -> Boolean = { it == null }
//): Flow<DataState<T>> = dataFlowCacheFirstInternal(
//    query = fromSuspend { query() }, // Adapt suspend fun to DataSource
//    fetch = fromSuspend { fetch() }, // Adapt suspend fun to DataSource
//    saveFetchResult = saveFetchResult,
//    shouldFetch = shouldFetch
//)
//
///**
// * Case 3: Both query and fetch operations return a Flow.
// * Can be useful if the remote source is a stream (e.g., gRPC, WebSocket).
// */
//inline fun <T : Any, R : Any> dataFlowCacheFirst(
//    crossinline query: () -> Flow<T?>,
//    crossinline fetch: () -> Flow<R?>,
//    crossinline saveFetchResult: suspend (R?) -> Unit,
//    crossinline shouldFetch: (T?) -> Boolean = { it == null }
//): Flow<DataState<T>> = dataFlowCacheFirstInternal(
//    query = query, // Already matches DataSource type
//    fetch = fetch, // Already matches DataSource type
//    saveFetchResult = saveFetchResult,
//    shouldFetch = shouldFetch
//)
//
///**
// * Case 4: A less common scenario.
// * Query from a local source with a suspend function.
// * Fetch from a remote source that returns a Flow.
// */
//inline fun <T : Any, R : Any> dataFlowCacheFirst(
//    crossinline query: suspend () -> T?,
//    crossinline fetch: () -> Flow<R?>,
//    crossinline saveFetchResult: suspend (R?) -> Unit,
//    crossinline shouldFetch: (T?) -> Boolean = { it == null }
//): Flow<DataState<T>> = dataFlowCacheFirstInternal(
//    query = fromSuspend { query() }, // Adapt suspend fun to DataSource
//    fetch = fetch, // Already matches DataSource type
//    saveFetchResult = saveFetchResult,
//    shouldFetch = shouldFetch
//)