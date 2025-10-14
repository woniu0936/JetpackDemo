package com.demo.jetpack.hilt

import com.demo.core.logger.logD
import javax.inject.Inject

class Truck @Inject constructor(val driver: Driver) {

    @BindGasEngine
    @Inject
    lateinit var mGasEngine: Engine

    @BindElectricEngine
    @Inject
    lateinit var mElectricEngine: Engine

    fun deliver() {
        mGasEngine.start()
        mElectricEngine.start()
        logD { "Truck is delivering cargo, Driven by $driver" }
        mGasEngine.shutdown()
        mElectricEngine.shutdown()
    }

}