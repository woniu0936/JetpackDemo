package com.demo.core.mmkv.di

import android.content.Context
import com.demo.core.mmkv.MMKVEventBus
import com.demo.core.mmkv.DefaultMMKVEventBus
import com.tencent.mmkv.MMKV
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MmkvModule {

    @Binds
    @Singleton
    abstract fun bindMMKVEventBus(impl: DefaultMMKVEventBus): MMKVEventBus

    companion object {
        /**
         * 提供 MMKV 的实例。
         * Hilt 会自动传入 ApplicationContext。
         * 这个方法只会在 App 生命周期内被调用一次。
         */
        @Provides
        @Singleton
        fun provideMMKV(
            @ApplicationContext context: Context
        ): MMKV {
            MMKV.initialize(context)
            return MMKV.defaultMMKV()
        }
    }
}