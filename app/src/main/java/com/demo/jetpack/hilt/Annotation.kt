package com.demo.jetpack.hilt

import javax.inject.Qualifier
import javax.inject.Scope

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class BindGasEngine

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class BindElectricEngine