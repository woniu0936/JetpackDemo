package com.demo.core.mmkv.di

import android.content.Context
import com.demo.core.mmkv.DefaultMMKVEventBus
import com.demo.core.mmkv.DefaultMMKVInitializer
import com.demo.core.mmkv.MMKVEventBus
import com.demo.core.mmkv.MMKVInitializer
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MmkvModule {

    /**
     * 提供 MMKV 的实例。
     * Hilt 会自动传入 ApplicationContext。
     * 这个方法只会在 App 生命周期内被调用一次。
     */
    @Provides
    @Singleton
    fun provideMMKV(
        @ApplicationContext context: Context,
        mmkvInitializer: MMKVInitializer
    ): MMKV {
        mmkvInitializer.initialize(context)
        return mmkvInitializer.defaultMMKV()
    }

    @Provides
    @Singleton
    fun provideMMKVEventBus(): MMKVEventBus {
        return DefaultMMKVEventBus()
    }

    @Provides
    @Singleton
    fun provideMMKVInitializer(): MMKVInitializer {
        return DefaultMMKVInitializer()
    }

}