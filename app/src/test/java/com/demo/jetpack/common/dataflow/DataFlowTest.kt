package com.demo.jetpack.common.dataflow

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class DataFlowTest {

    // region DataCacheFirst Tests

    /**
     * 测试场景: 本地有缓存数据，且策略 (`shouldFetchRemote`) 配置为不需要从远程获取。
     * 预期结果: 只发射本地数据("local")，然后流程正常完成。
     */
    @Test
    fun `dataCacheFirst - local success, should not fetch remote - emits local only`() = runTest {
        dataCacheFirst(
            isOnline = { true },
            local = { "local" },
            remote = { "remote" },
            cacheRemote = {},
            shouldFetchRemote = { false },
            shouldEmitRemote = { _, _ -> false }
        ).test {
            assertThat(awaitItem()).isEqualTo("local")
            awaitComplete()
        }
    }

    /**
     * 测试场景: 本地有缓存数据，策略允许远程获取，且远程获取成功。
     * 预期结果: 首先立即发射本地数据("local")，然后发射远程获取到的新数据("remote")，最后流程完成。
     */
    @Test
    fun `dataCacheFirst - local success, should fetch remote, remote success - emits local then remote`() = runTest {
        var cache = "local"
        dataCacheFirst(
            isOnline = { true },
            local = { cache },
            remote = { "remote" },
            cacheRemote = { data -> cache = data },
            shouldFetchRemote = { true },
            shouldEmitRemote = { _, _ -> true }
        ).test {
            assertThat(awaitItem()).isEqualTo("local")
            assertThat(awaitItem()).isEqualTo("remote")
            awaitComplete()
        }
    }

    /**
     * 测试场景: 本地有缓存数据，策略允许远程获取，但远程获取失败（例如网络错误）。
     * 预期结果: 只发射本地数据("local")，并静默处理远程错误，然后流程正常完成。
     */
    @Test
    fun `dataCacheFirst - local success, should fetch remote, remote fails - emits local and completes`() = runTest {
        dataCacheFirst(
            isOnline = { true },
            local = { "local" },
            remote = { throw IOException("Network error") },
            cacheRemote = {},
            shouldFetchRemote = { true },
            shouldEmitRemote = { _, _ -> true }
        ).test {
            assertThat(awaitItem()).isEqualTo("local")
            awaitComplete()
        }
    }

    /**
     * 测试场景: 本地无缓存数据，但远程获取成功。
     * 预期结果: 发射远程获取到的数据("remote")，然后流程正常完成。
     */
    @Test
    fun `dataCacheFirst - local empty, remote success - emits remote`() = runTest {
        var cache: String? = null
        dataCacheFirst(
            isOnline = { true },
            local = { cache },
            remote = { "remote" },
            cacheRemote = { data -> cache = data },
            shouldFetchRemote = { true },
            shouldEmitRemote = { _, _ -> true }
        ).test {
            assertThat(awaitItem()).isEqualTo("remote")
            awaitComplete()
        }
    }

    /**
     * 测试场景: 本地无缓存数据，且设备处于离线状态。
     * 预期结果: 流程因无法提供任何数据而失败，抛出 `NetworkUnavailableException`。
     */
    @Test
    fun `dataCacheFirst - local empty, offline - throws NetworkUnavailableException`() = runTest {
        dataCacheFirst<String>(
            isOnline = { false },
            local = { null },
            remote = { "remote" },
            cacheRemote = {},
            shouldFetchRemote = { true },
            shouldEmitRemote = { _, _ -> true }
        ).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(NetworkUnavailableException::class.java)
        }
    }

    /**
     * 测试场景: 本地无缓存数据，远程获取失败（例如网络错误）。
     * 预期结果: 流程因无法提供任何数据而失败，抛出 `RemoteFailedException`。
     */
    @Test
    fun `dataCacheFirst - local empty, remote fails - throws RemoteFailedException`() = runTest {
        dataCacheFirst<String>(
            isOnline = { true },
            local = { null },
            remote = { throw IOException("Network error") },
            cacheRemote = {},
            shouldFetchRemote = { true },
            shouldEmitRemote = { _, _ -> true }
        ).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(RemoteFailedException::class.java)
            assertThat(error.cause).isInstanceOf(IOException::class.java)
        }
    }

    /**
     * 测试场景: 本地无缓存数据，远程获取成功但返回 `null`（表示远程无此数据）。
     * 预期结果: 流程因无法提供任何数据而失败，抛出 `RemoteEmptyException`。
     */
    @Test
    fun `dataCacheFirst - local empty, remote returns null - throws RemoteEmptyException`() = runTest {
        dataCacheFirst<String>(
            isOnline = { true },
            local = { null },
            remote = { null },
            cacheRemote = {},
            shouldFetchRemote = { true },
            shouldEmitRemote = { _, _ -> true }
        ).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(RemoteEmptyException::class.java)
        }
    }

    // endregion

    // region DataCacheFirst Reactive Tests

    /**
     * 测试场景 (响应式): 初始缓存存在，且策略禁止远程获取。
     * 预期结果: 发射初始缓存数据("local")，然后保持监听状态等待后续更新（测试中通过 `cancelAndIgnoreRemainingEvents` 结束）。
     */
    @Test
    fun `dataCacheFirst reactive - initial cache, no fetch - emits initial cache and awaits`() = runTest {
        val localFlow = flowOf("local")
        dataCacheFirstFlow(
            isOnline = { true },
            local = { localFlow },
            remote = { "remote" },
            cacheRemote = {},
            shouldFetchRemote = { false }
        ).test {
            assertThat(awaitItem()).isEqualTo("local")
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * 测试场景 (响应式): 初始缓存为空，远程获取成功并更新缓存。
     * 预期结果: 远程数据会更新缓存，然后本地的响应式Flow会发射新的缓存数据。测试中忽略了具体发射值，仅验证流程不中断。
     */
    @Test
    fun `dataCacheFirst reactive - no initial cache, remote success - caches and emits from local`() = runTest {
        var cache: String? = null
        val localFlow = flow<String?> { emit(cache) }
        dataCacheFirstFlow(
            isOnline = { true },
            local = { localFlow },
            remote = { "remote" },
            cacheRemote = { data -> cache = data },
            shouldFetchRemote = { true }
        ).test {
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * 测试场景 (响应式): 初始缓存为空，且设备离线。
     * 预期结果: 流程因无法提供任何数据而失败，抛出 `NetworkUnavailableException`。
     */
    @Test
    fun `dataCacheFirst reactive - no initial cache, offline - throws NetworkUnavailableException`() = runTest {
        dataCacheFirstFlow<String>(
            isOnline = { false },
            local = { flow { emit(null) } },
            remote = { "remote" },
            cacheRemote = {},
            shouldFetchRemote = { true }
        ).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(NetworkUnavailableException::class.java)
        }
    }

    // endregion

    // region DataNetworkFirst Tests

    /**
     * 测试场景: 设备在线，远程获取成功。
     * 预期结果: 发射远程数据("remote")，并将其存入缓存，然后流程完成。
     */
    @Test
    fun `dataNetworkFirst - online, remote success - emits remote`() = runTest {
        var cache: String? = null
        dataNetworkFirst(
            isOnline = { true },
            local = { cache },
            remote = { "remote" },
            cacheRemote = { data -> cache = data },
            shouldEmitLocal = { true }
        ).test {
            assertThat(awaitItem()).isEqualTo("remote")
            awaitComplete()
            assertThat(cache).isEqualTo("remote")
        }
    }

    /**
     * 测试场景: 设备在线，但远程获取失败，此时回退到本地缓存且缓存存在。
     * 预期结果: 发射本地缓存数据("local")，然后流程完成。
     */
    @Test
    fun `dataNetworkFirst - online, remote fails, local success - emits local`() = runTest {
        dataNetworkFirst(
            isOnline = { true },
            local = { "local" },
            remote = { throw IOException("Network error") },
            cacheRemote = {},
            shouldEmitLocal = { true }
        ).test {
            assertThat(awaitItem()).isEqualTo("local")
            awaitComplete()
        }
    }

    /**
     * 测试场景: 设备离线，但本地缓存存在。
     * 预期结果: 直接从本地缓存获取并发射数据("local")，然后流程完成。
     */
    @Test
    fun `dataNetworkFirst - offline, local success - emits local`() = runTest {
        dataNetworkFirst(
            isOnline = { false },
            local = { "local" },
            remote = { "remote" },
            cacheRemote = {},
            shouldEmitLocal = { true }
        ).test {
            assertThat(awaitItem()).isEqualTo("local")
            awaitComplete()
        }
    }

    /**
     * 测试场景: 设备在线，远程获取成功但返回`null`，此时回退到本地缓存且缓存存在。
     * 预期结果: 发射本地缓存数据("local")，然后流程完成。
     */
    @Test
    fun `dataNetworkFirst - online, remote returns null, local success - emits local`() = runTest {
        dataNetworkFirst(
            isOnline = { true },
            local = { "local" },
            remote = { null },
            cacheRemote = {},
            shouldEmitLocal = { true }
        ).test {
            assertThat(awaitItem()).isEqualTo("local")
            awaitComplete()
        }
    }

    /**
     * 测试场景: 设备在线，远程获取成功但返回`null`，且本地缓存也为空。
     * 预期结果: 发射`null`表示未找到数据，然后流程完成。
     */
    @Test
    fun `dataNetworkFirst - online, remote returns null, local empty - emits null`() = runTest {
        dataNetworkFirst<String>(
            isOnline = { true },
            local = { null },
            remote = { null },
            cacheRemote = {},
            shouldEmitLocal = { true }
        ).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    /**
     * 测试场景: 远程获取失败，回退到本地缓存也失败。
     * 预期结果: 流程因无法提供任何数据而失败，抛出 `RemoteFailedException`。
     */
    @Test
    fun `dataNetworkFirst - remote fails, local fails - throws RemoteFailedException`() = runTest {
        dataNetworkFirst<String>(
            isOnline = { true },
            local = { throw IllegalStateException("Database error") },
            remote = { throw IOException("Network error") },
            cacheRemote = {},
            shouldEmitLocal = { true }
        ).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(RemoteFailedException::class.java)
            assertThat(error.cause).isInstanceOf(IOException::class.java)
        }
    }

    /**
     * 测试场景: 设备离线，且本地缓存获取失败。
     * 预期结果: 发射`null`表示未找到数据，然后流程完成。
     */
    @Test
    fun `dataNetworkFirst - offline, local fails - emits null`() = runTest {
        dataNetworkFirst(
            isOnline = { false },
            local = { throw IllegalStateException("Database error") },
            remote = { "remote" },
            cacheRemote = {},
            shouldEmitLocal = { true }
        ).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    // endregion

    // region catchInitialError Tests

    /**
     * 测试场景: Flow 抛出 NetworkUnavailableException。
     * 预期结果: onNetworkUnavailable lambda 被调用，并且 Flow 正常完成（因为错误被捕获处理）。
     */
    @Test
    fun `catchInitialError - catches NetworkUnavailableException`() = runTest {
        var networkUnavailableHandled = false
        flow<String> { throw NetworkUnavailableException("req1") }
            .catchInitialError(
                onNetworkUnavailable = { networkUnavailableHandled = true }
            )
            .test {
                awaitComplete()
            }
        assertThat(networkUnavailableHandled).isTrue()
    }

    /**
     * 测试场景: Flow 抛出 RemoteEmptyException。
     * 预期结果: onRemoteEmpty lambda 被调用，并且 Flow 正常完成。
     */
    @Test
    fun `catchInitialError - catches RemoteEmptyException`() = runTest {
        var remoteEmptyHandled = false
        flow<String> { throw RemoteEmptyException("req2") }
            .catchInitialError(
                onRemoteEmpty = { remoteEmptyHandled = true }
            )
            .test {
                awaitComplete()
            }
        assertThat(remoteEmptyHandled).isTrue()
    }

    /**
     * 测试场景: Flow 抛出 RemoteFailedException。
     * 预期结果: onRemoteFailed lambda 被调用，并且 Flow 正常完成。
     */
    @Test
    fun `catchInitialError - catches RemoteFailedException`() = runTest {
        var remoteFailedHandled = false
        flow<String> { throw RemoteFailedException("req3", IOException("Remote failed")) }
            .catchInitialError(
                onRemoteFailed = { remoteFailedHandled = true }
            )
            .test {
                awaitComplete()
            }
        assertThat(remoteFailedHandled).isTrue()
    }

    /**
     * 测试场景: Flow 抛出非 InitialDataLoadException 类型的未知异常。
     * 预期结果: onUnknown lambda 被调用，并且 Flow 正常完成。
     */
    @Test
    fun `catchInitialError - catches unknown exception`() = runTest {
        var unknownHandled = false
        val genericException = IllegalStateException("Something unexpected")
        flow<String> { throw genericException }
            .catchInitialError(
                onUnknown = { error ->
                    unknownHandled = true
                    assertThat(error).isEqualTo(genericException)
                }
            )
            .test {
                awaitComplete()
            }
        assertThat(unknownHandled).isTrue()
    }

    /**
     * 测试场景: Flow 成功发射数据，没有抛出任何异常。
     * 预期结果: Flow 正常发射数据并完成，没有任何错误处理 lambda 被调用。
     */
    @Test
    fun `catchInitialError - no error, emits data normally`() = runTest {
        var anyErrorHandled = false
        flowOf("Success Data")
            .catchInitialError(
                onNetworkUnavailable = { anyErrorHandled = true },
                onRemoteEmpty = { anyErrorHandled = true },
                onRemoteFailed = { anyErrorHandled = true },
                onUnknown = { anyErrorHandled = true }
            )
            .test {
                assertThat(awaitItem()).isEqualTo("Success Data")
                awaitComplete()
            }
        assertThat(anyErrorHandled).isFalse()
    }

    /**
     * 测试场景: Flow 抛出异常，但 catchInitialError 没有提供任何处理 lambda（使用默认空实现）。
     * 预期结果: 异常被捕获，Flow 正常完成，不会崩溃。
     */
    @Test
    fun `catchInitialError - with default empty lambdas, catches error and completes`() = runTest {
        flow<String> { throw NetworkUnavailableException("req4") }
            .catchInitialError() // Using default empty lambdas
            .test {
                awaitComplete()
            }
        // No assertion on handled flag, just that it completes without crashing
    }

    // endregion
}
