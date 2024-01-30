package com.demo.jetpack.core.extension

val Boolean?.isTrue
    get() = this == true

val Boolean?.isFalse
    get() = this != true

inline fun Boolean?.ifTrue(block: () -> Unit) {
    if (isTrue) {
        block()
    }
}

inline fun Boolean?.ifFalse(block: () -> Unit) {
    if (isFalse) {
        block()
    }
}