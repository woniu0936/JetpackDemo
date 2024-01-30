package com.demo.jetpack.core.extension

val Int.SECONDS
    get() = this * 1000L

val Int.MINUTES
    get() = this * 60 * 1000L

val Int.HOURS
    get() = this * 60 * 60 * 1000L