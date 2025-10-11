package com.demo.core.mmkv

import android.content.Context
import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

interface MMKVInitializer {
    fun initialize(context: Context)
    fun defaultMMKV(): MMKV
}

@Singleton
class DefaultMMKVInitializer @Inject constructor() : MMKVInitializer {
    override fun initialize(context: Context) {
        MMKV.initialize(context)
    }

    override fun defaultMMKV(): MMKV {
        return MMKV.defaultMMKV()
    }
}
