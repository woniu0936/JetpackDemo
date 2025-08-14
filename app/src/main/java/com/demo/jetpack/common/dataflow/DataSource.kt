//package com.demo.jetpack.common.dataflow
//
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//
///**
// * A typealias representing a data source strategy.
// * It abstracts any data source (suspend fun, Flow, etc.) into a simple function
// * that returns a Flow. This is the core of the Dependency Inversion Principle here.
// * @param T The type of data the source provides.
// */
//typealias DataSource<T> = () -> Flow<T?>
//
///**
// * A standalone adapter utility function.
// * It takes a suspend function and converts it into our unified DataSource interface.
// * @param block The suspend function to be adapted.
// * @return A DataSource function (a lambda) that wraps the suspend call in a flow.
// */
//inline fun <T> fromSuspend(
//    crossinline block: suspend () -> T?
//): DataSource<T> = {
//    flow {
//        emit(block())
//    }
//}