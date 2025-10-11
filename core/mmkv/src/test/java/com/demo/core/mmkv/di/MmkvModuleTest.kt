package com.demo.core.mmkv.di

import android.content.Context
import com.tencent.mmkv.MMKV
import io.mockk.every

import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `MmkvModule` 的单元测试类。
 * 旨在验证 `MmkvModule` 中 Hilt 提供的依赖项是否正确初始化和配置。
 */
class MmkvModuleTest {

    // 模拟 Android Context，用于 Hilt 的 @ApplicationContext 注入。
    private val mockContext: Context = mockk(relaxed = true)

    /**
     * 测试 `provideMMKV` 方法是否正确初始化 MMKV 并返回其实例。
     *
     * 预期结果：
     * 1. `MMKV.initialize` 方法被调用，传入正确的 Context。
     * 2. `MMKV.defaultMMKV` 方法被调用。
     * 3. `provideMMKV` 返回一个非空的 MMKV 实例。
     */
    @Test
    fun provideMMKV_initializesAndReturnsMMKVInstance() {
        // Given (准备阶段)
        // 创建一个模拟的 MMKV 实例，这是 provideMMKV 预期返回的对象。
        val mockMMKVInstance = mockk<MMKV>()

        // 使用 MockK 模拟 MMKV 的静态方法
        mockkStatic(MMKV::class) {
            // 桩接 (Stub) MMKV 的行为：
            // 当调用 initialize 方法时，不执行任何操作。
            every { MMKV.initialize(any<Context>()) } answers { nothing }
            // 当调用 defaultMMKV 方法时，返回我们预设的 mockMMKVInstance。
            every { MMKV.defaultMMKV() } returns mockMMKVInstance

            // When (执行阶段)
            // 调用 MmkvModule 的 provideMMKV 方法，传入模拟的 Context。
            val mmkv = MmkvModule.Companion.provideMMKV(mockContext)

            // Then (验证阶段)
            // 验证返回的 MMKV 实例不为空。
            assertNotNull(mmkv)
            // 验证 MMKV.initialize 方法被调用了一次，且传入了 mockContext。
            verify(exactly = 1) { MMKV.initialize(mockContext) }
            // 验证 MMKV.defaultMMKV 方法被调用了一次。
            verify(exactly = 1) { MMKV.defaultMMKV() }
            // 验证返回的 MMKV 实例就是我们模拟的那个实例。
            assertTrue(mmkv === mockMMKVInstance)
        }
    }
}