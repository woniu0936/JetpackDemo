package com.demo.jetpack.hilt

import com.demo.jetpack.extension.logD
import javax.inject.Inject

class ElectricEngine @Inject constructor() : Engine {

    override fun start() {
        logD { "ElectricEngine engine start" }
    }

    override fun shutdown() {
        logD { "ElectricEngine engine shutdown" }
    }
}