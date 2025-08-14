//package com.demo.jetpack.common.dataflow
//
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.distinctUntilChanged
//import kotlinx.coroutines.flow.filterIsInstance
//import kotlinx.coroutines.flow.map
//
///**
// * A generic sealed interface that encapsulates the various states of an asynchronous operation.
// * This is the single source of truth for the state of data requested by the UI.
// */
//sealed interface DataState<out T> {
//    /**
//     * Represents a successful outcome with non-null data.
//     * @param data The retrieved non-null data.
//     */
//    data class Success<T>(val data: T) : DataState<T>
//
//    /**
//     * Represents a failure outcome with an exception.
//     * @param exception The caught exception.
//     */
//    data class Error(val exception: Throwable) : DataState<Nothing>
//
//    /**
//     * Represents a successful outcome but with no data returned.
//     * This is crucial for distinguishing between "not found" and an actual error.
//     */
//    object Empty : DataState<Nothing>
//
//    /**
//     * Represents an in-progress operation.
//     */
//    object Loading : DataState<Nothing>
//}
//
///**
// * A generic extension function to map the data within a `Flow<Resource<T>>`
// * to a `Flow<Resource<R>>` while preserving the other states (`Loading`, `Empty`, `Error`).
// *
// * This utility prevents repetitive `when` statements in ViewModels.
// *
// * @param T The original data type inside the Resource.
// * @param R The target data type inside the Resource.
// * @param transform A suspend function to transform the data from T to R.
// * @return A new Flow emitting the mapped Resource.
// */
//inline fun <T, R> Flow<DataState<T>>.mapResource(
//    crossinline transform: suspend (T) -> R
//): Flow<DataState<R>> {
//    return this.map { resource ->
//        when (resource) {
//            is DataState.Success -> DataState.Success(transform(resource.data))
//            is DataState.Error -> resource
//            is DataState.Empty -> resource
//            is DataState.Loading -> resource
//        }
//    }
//}
//
///**
// * A generic, chainable, and intermediate Flow operator that transforms a
// * `Flow<Resource<T>>` into a `Flow<R>` of a specific property from the `Success` state.
// *
// * It elegantly handles filtering for the `Success` state and ensuring that emissions only
// * occur when the selected property has actually changed.
// *
// * This is primarily used in the UI layer (Fragment/Activity) to observe changes
// * in a specific part of the UI state without re-rendering the entire screen.
// *
// * @param T The type of the data object within the `Success` state.
// * @param R The type of the property to observe.
// * @param property A lambda that selects a property of type R from the data object T.
// * @return A new `Flow<R>` that only emits when the selected property of a `Success` state changes.
// */
//fun <T, R> Flow<DataState<T>>.mapOnSuccess(
//    property: (T) -> R
//): Flow<R> {
//    return this
//        .filterIsInstance<DataState.Success<T>>() // 1. Only let `Success` states pass through.
//        .map { success -> property(success.data) }   // 2. Pluck the desired property from the `Success` data.
//        .distinctUntilChanged()                      // 3. IMPORTANT: Only emit when the property itself has changed.
//}
//
//// ====================================================================================
//// --- Inline Extension Functions for Handling Resource States ---
//// ====================================================================================
//
///**
// * Executes the given [action] if this [DataState] is a [DataState.Success].
// * Returns the original [DataState] instance.
// */
//inline fun <T> DataState<T>.onSuccess(
//    action: (data: T) -> Unit
//): DataState<T> {
//    if (this is DataState.Success) {
//        action(data)
//    }
//    return this
//}
//
///**
// * Executes the given [action] if this [DataState] is a [DataState.Error].
// * Returns the original [DataState] instance.
// */
//inline fun <T> DataState<T>.onFailure(
//    action: (exception: Throwable) -> Unit
//): DataState<T> {
//    if (this is DataState.Error) {
//        action(exception)
//    }
//    return this
//}
//
///**
// * Executes the given [action] if this [DataState] is [DataState.Empty].
// * Returns the original [DataState] instance.
// */
//inline fun <T> DataState<T>.onEmpty(
//    action: () -> Unit
//): DataState<T> {
//    if (this is DataState.Empty) {
//        action()
//    }
//    return this
//}
//
///**
// * Executes the given [action] if this [DataState] is [DataState.Loading].
// * Returns the original [DataState] instance.
// */
//inline fun <T> DataState<T>.onLoading(
//    action: () -> Unit
//): DataState<T> {
//    if (this is DataState.Loading) {
//        action()
//    }
//    return this
//}
//
///**
// * Executes the given [action] if this [DataState] is either a [DataState.Error] or [DataState.Empty].
// * This is useful for handling cases where "not found" and "failure" lead to the same UI state (e.g., showing an error/empty view).
// * The action receives a nullable [Throwable] to distinguish between the two states if needed.
// *
// * @param action The action to execute. It receives the exception if the state is [DataState.Error], otherwise it receives `null`.
// * @return The original [DataState] instance for chaining.
// */
//inline fun <T> DataState<T>.onFailureOrEmpty(
//    action: (exception: Throwable?) -> Unit
//): DataState<T> {
//    when (this) {
//        is DataState.Error -> action(exception)
//        is DataState.Empty -> action(null)
//        else -> { /* Do nothing */ }
//    }
//    return this
//}
//
//// ====================================================================================
//// --- Inline Extension Functions for Retrieving Data ---
//// ====================================================================================
//
///**
// * Returns the encapsulated data if this instance represents [DataState.Success] or `null`
// * if it is [DataState.Error], [DataState.Empty], or [DataState.Loading].
// */
//fun <T> DataState<T>.getOrNull(): T? {
//    return (this as? DataState.Success)?.data
//}
//
///**
// * Returns the encapsulated [Throwable] exception if this instance represents [DataState.Error] or `null`
// * if it is [DataState.Success], [DataState.Empty], or [DataState.Loading].
// */
//fun <T> DataState<T>.exceptionOrNull(): Throwable? {
//    return (this as? DataState.Error)?.exception
//}
//
//
//// ====================================================================================
//// --- Extension Functions for Transformation ---
//// ====================================================================================
//
///**
// * Returns a new [DataState] with the data transformed by [transform] if it is a [DataState.Success].
// * The other states ([Error], [Empty], [Loading]) are preserved.
// */
//inline fun <T, R> DataState<T>.map(
//    transform: (T) -> R
//): DataState<R> {
//    return when (this) {
//        is DataState.Success -> DataState.Success(transform(data))
//        is DataState.Error -> this
//        is DataState.Empty -> this
//        is DataState.Loading -> this
//    }
//}
//
///**
// * Folds all states of this [DataState] into a single value of type [R].
// * This provides a way to handle all possible outcomes in a single expression.
// */
//inline fun <T, R> DataState<T>.fold(
//    onSuccess: (data: T) -> R,
//    onError: (exception: Throwable) -> R,
//    onEmpty: () -> R,
//    onLoading: () -> R
//): R {
//    return when (this) {
//        is DataState.Success -> onSuccess(data)
//        is DataState.Error -> onError(exception)
//        is DataState.Empty -> onEmpty()
//        is DataState.Loading -> onLoading()
//    }
//}