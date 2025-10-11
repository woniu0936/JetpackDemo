package com.demo.core.mmkv.di

import android.content.Context
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
    fun provideMMKV(@ApplicationContext context: Context): MMKV {
        // 在这里执行 MMKV 的初始化
        MMKV.initialize(context)
        return MMKV.defaultMMKV()
    }

}