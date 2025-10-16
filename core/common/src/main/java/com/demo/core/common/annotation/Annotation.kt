package com.demo.core.common.annotation

import javax.inject.Qualifier

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
