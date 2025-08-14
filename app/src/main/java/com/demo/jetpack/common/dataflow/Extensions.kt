//package com.demo.jetpack.common.dataflow
//
//import com.demo.jetpack.common.dataflow.DataState
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.distinctUntilChanged
//import kotlinx.coroutines.flow.filterIsInstance
//import kotlinx.coroutines.flow.map
//import kotlin.reflect.KProperty1
//
//fun <T> Flow<DataState<T>>.observeOnSuccess(): Flow<T> {
//    return this
//        .filterIsInstance<DataState.Success<T>>()
//        .map { success -> success.data }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 1 Properties]
// */
//fun <T, P1> Flow<DataState<T>>.observeOnSuccess(
//    prop: KProperty1<T, P1>,
//): Flow<P1> {
//    return this
//        .filterIsInstance<DataState.Success<T>>()
//        .map { success -> prop.get(success.data) }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 2 Properties]
// */
//fun <T, P1, P2> Flow<DataState<T>>.observeOnSuccess(
//    prop1: KProperty1<T, P1>,
//    prop2: KProperty1<T, P2>
//): Flow<Pair<P1, P2>> {
//    return this.filterIsInstance<DataState.Success<T>>()
//        .map { success -> Pair(prop1.get(success.data), prop2.get(success.data)) }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 3 Properties]
// */
//fun <T, P1, P2, P3> Flow<DataState<T>>.observeOnSuccess(
//    prop1: KProperty1<T, P1>,
//    prop2: KProperty1<T, P2>,
//    prop3: KProperty1<T, P3>
//): Flow<Triple<P1, P2, P3>> {
//    return this.filterIsInstance<DataState.Success<T>>()
//        .map { success -> Triple(prop1.get(success.data), prop2.get(success.data), prop3.get(success.data)) }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 4 Properties]
// */
//fun <T, P1, P2, P3, P4> Flow<DataState<T>>.observeOnSuccess(
//    prop1: KProperty1<T, P1>,
//    prop2: KProperty1<T, P2>,
//    prop3: KProperty1<T, P3>,
//    prop4: KProperty1<T, P4>
//): Flow<Tuple4<P1, P2, P3, P4>> {
//    return this.filterIsInstance<DataState.Success<T>>()
//        .map { success -> Tuple4(prop1.get(success.data), prop2.get(success.data), prop3.get(success.data), prop4.get(success.data)) }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 5 Properties]
// */
//fun <T, P1, P2, P3, P4, P5> Flow<DataState<T>>.observeOnSuccess(
//    prop1: KProperty1<T, P1>,
//    prop2: KProperty1<T, P2>,
//    prop3: KProperty1<T, P3>,
//    prop4: KProperty1<T, P4>,
//    prop5: KProperty1<T, P5>
//): Flow<Tuple5<P1, P2, P3, P4, P5>> {
//    return this
//        .filterIsInstance<DataState.Success<T>>()
//        .map { success ->
//            Tuple5(
//                prop1.get(success.data), prop2.get(success.data),
//                prop3.get(success.data), prop4.get(success.data),
//                prop5.get(success.data)
//            )
//        }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 6 Properties]
// */
//fun <T, P1, P2, P3, P4, P5, P6> Flow<DataState<T>>.observeOnSuccess(
//    prop1: KProperty1<T, P1>, prop2: KProperty1<T, P2>, prop3: KProperty1<T, P3>,
//    prop4: KProperty1<T, P4>, prop5: KProperty1<T, P5>, prop6: KProperty1<T, P6>
//): Flow<Tuple6<P1, P2, P3, P4, P5, P6>> {
//    return this
//        .filterIsInstance<DataState.Success<T>>()
//        .map { success ->
//            Tuple6(
//                prop1.get(success.data), prop2.get(success.data), prop3.get(success.data),
//                prop4.get(success.data), prop5.get(success.data), prop6.get(success.data)
//            )
//        }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 7 Properties]
// */
//fun <T, P1, P2, P3, P4, P5, P6, P7> Flow<DataState<T>>.observeOnSuccess(
//    prop1: KProperty1<T, P1>, prop2: KProperty1<T, P2>, prop3: KProperty1<T, P3>,
//    prop4: KProperty1<T, P4>, prop5: KProperty1<T, P5>, prop6: KProperty1<T, P6>,
//    prop7: KProperty1<T, P7>
//): Flow<Tuple7<P1, P2, P3, P4, P5, P6, P7>> {
//    return this
//        .filterIsInstance<DataState.Success<T>>()
//        .map { success ->
//            Tuple7(
//                prop1.get(success.data), prop2.get(success.data), prop3.get(success.data),
//                prop4.get(success.data), prop5.get(success.data), prop6.get(success.data),
//                prop7.get(success.data)
//            )
//        }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 8 Properties]
// */
//fun <T, P1, P2, P3, P4, P5, P6, P7, P8> Flow<DataState<T>>.observeOnSuccess(
//    prop1: KProperty1<T, P1>, prop2: KProperty1<T, P2>, prop3: KProperty1<T, P3>,
//    prop4: KProperty1<T, P4>, prop5: KProperty1<T, P5>, prop6: KProperty1<T, P6>,
//    prop7: KProperty1<T, P7>, prop8: KProperty1<T, P8>
//): Flow<Tuple8<P1, P2, P3, P4, P5, P6, P7, P8>> {
//    return this
//        .filterIsInstance<DataState.Success<T>>()
//        .map { success ->
//            Tuple8(
//                prop1.get(success.data), prop2.get(success.data), prop3.get(success.data),
//                prop4.get(success.data), prop5.get(success.data), prop6.get(success.data),
//                prop7.get(success.data), prop8.get(success.data)
//            )
//        }
//        .distinctUntilChanged()
//}
//
///**
// * [API for 9 Properties]
// */
//fun <T, P1, P2, P3, P4, P5, P6, P7, P8, P9> Flow<DataState<T>>.observeOnSuccess(
//    prop1: KProperty1<T, P1>, prop2: KProperty1<T, P2>, prop3: KProperty1<T, P3>,
//    prop4: KProperty1<T, P4>, prop5: KProperty1<T, P5>, prop6: KProperty1<T, P6>,
//    prop7: KProperty1<T, P7>, prop8: KProperty1<T, P8>, prop9: KProperty1<T, P9>
//): Flow<Tuple9<P1, P2, P3, P4, P5, P6, P7, P8, P9>> {
//    return this
//        .filterIsInstance<DataState.Success<T>>()
//        .map { success ->
//            Tuple9(
//                prop1.get(success.data), prop2.get(success.data), prop3.get(success.data),
//                prop4.get(success.data), prop5.get(success.data), prop6.get(success.data),
//                prop7.get(success.data), prop8.get(success.data), prop9.get(success.data)
//            )
//        }
//        .distinctUntilChanged()
//}
//
//data class Tuple4<out A, out B, out C, out D>(
//    val first: A,
//    val second: B,
//    val third: C,
//    val fourth: D,
//)
//
//data class Tuple5<out A, out B, out C, out D, out E>(
//    val first: A,
//    val second: B,
//    val third: C,
//    val fourth: D,
//    val fifth: E,
//)
//
//data class Tuple6<out A, out B, out C, out D, out E, out F>(
//    val first: A,
//    val second: B,
//    val third: C,
//    val fourth: D,
//    val fifth: E,
//    val sixth: F,
//)
//
//data class Tuple7<out A, out B, out C, out D, out E, out F, out G>(
//    val first: A,
//    val second: B,
//    val third: C,
//    val fourth: D,
//    val fifth: E,
//    val sixth: F,
//    val seventh: G,
//)
//
//data class Tuple8<out A, out B, out C, out D, out E, out F, out G, out H>(
//    val first: A,
//    val second: B,
//    val third: C,
//    val fourth: D,
//    val fifth: E,
//    val sixth: F,
//    val seventh: G,
//    val eighth: H,
//)
//
//data class Tuple9<out A, out B, out C, out D, out E, out F, out G, out H, out I>(
//    val first: A,
//    val second: B,
//    val third: C,
//    val fourth: D,
//    val fifth: E,
//    val sixth: F,
//    val seventh: G,
//    val eighth: H,
//    val ninth: I,
//)
//
