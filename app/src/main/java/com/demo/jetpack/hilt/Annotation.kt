package com.demo.jetpack.hilt

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BindGasEngine {

}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BindElectricEngine {

}