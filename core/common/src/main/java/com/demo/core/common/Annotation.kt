package com.demo.core.common

import javax.inject.Qualifier
import javax.inject.Scope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: AppDispatchers)

enum class AppDispatchers {
    IO,
    Default,
    Main
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope
