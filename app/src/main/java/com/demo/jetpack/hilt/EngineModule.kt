package com.demo.jetpack.hilt

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class EngineModule {

    @Binds
    @BindGasEngine
    abstract fun bindGasEngine(gasEngine: GasEngine):Engine

    @Binds
    @BindElectricEngine
    abstract fun bindElectricEngine(gasEngine: ElectricEngine):Engine

}