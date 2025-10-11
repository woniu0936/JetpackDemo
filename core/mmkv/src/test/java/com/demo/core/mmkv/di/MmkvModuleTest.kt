package com.demo.core.mmkv.di

import android.content.Context
import com.demo.core.mmkv.MMKVInitializer
import com.tencent.mmkv.MMKV
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class MmkvModuleTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockMMKVInitializer: MMKVInitializer

    @Test
    fun provideMMKV_initializesAndReturnsMMKVInstance() {
        // Given
        val mockMMKVInstance = mock(MMKV::class.java)

        // Stub MMKVInitializer behavior
        doNothing().whenever(mockMMKVInitializer).initialize(mockContext)
        whenever(mockMMKVInitializer.defaultMMKV()).thenReturn(mockMMKVInstance)

        // When
        val mmkv = MmkvModule.provideMMKV(mockContext, mockMMKVInitializer)

        // Then
        assertNotNull(mmkv)
        verify(mockMMKVInitializer).initialize(mockContext)
        verify(mockMMKVInitializer).defaultMMKV()
        assert(mmkv === mockMMKVInstance)
    }
}