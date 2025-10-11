package com.demo.core.mmkv

import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface MMKVEventBus {
    val events: MutableSharedFlow<Pair<String, Any?>>
    fun notify(key: String, value: Any?)
}

@Singleton
class DefaultMMKVEventBus @Inject constructor() : MMKVEventBus {
    override val events = MutableSharedFlow<Pair<String, Any?>>(replay = 1)
    override fun notify(key: String, value: Any?) {
        events.tryEmit(key to value)
    }
}
