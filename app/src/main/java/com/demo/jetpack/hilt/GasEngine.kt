package com.demo.jetpack.hilt

import com.demo.core.logger.logD
import javax.inject.Inject

class GasEngine @Inject constructor(): Engine {

    override fun start() {
        logD { "Gas engine start" }
    }

    override fun shutdown() {
        logD { "Gas engine shutdown" }
    }
}