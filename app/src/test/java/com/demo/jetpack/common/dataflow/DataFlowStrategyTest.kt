package com.demo.jetpack.common.dataflow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

@ExperimentalCoroutinesApi
class DataFlowStrategyTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private var isOnline: Boolean = true
    private var localData: String? = null
    private var remoteData: String? = null
    private var cachedRemoteData: String? = null
    private val localFlow = MutableSharedFlow<String?>(replay = 1)

    @Before
    fun setup() = testScope.runTest {
        isOnline = true
        localData = null
        remoteData = null
        cachedRemoteData = null
        localFlow.emit(null) // Reset local flow for each test
    }

    //region dataCacheFirstInternal (suspend local) tests
    @Test
    fun `cacheFirst_suspendLocal_localDataExists_emitsLocalThenRemote`() = testScope.runTest {
        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val results = flow.take(2).toList() // Expect local then remote

        assertEquals(listOf("local_initial", "remote_new"), results)
        assertEquals("remote_new", cachedRemoteData)
    }

    @Test
    fun `cacheFirst_suspendLocal_noLocalData_emitsRemote`() = testScope.runTest {
        remoteData = "remote_new"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("remote_new", result)
        assertEquals("remote_new", cachedRemoteData)
    }

    @Test
    fun `cacheFirst_suspendLocal_offline_localDataExists_emitsLocalOnly`() = testScope.runTest {
        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertNull(cachedRemoteData) // Remote should not be called
    }

    @Test(expected = NetworkUnavailableException::class)
    fun `cacheFirst_suspendLocal_offline_noLocalData_throwsNetworkUnavailableException`() = testScope.runTest {
        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        flow.first() // This should throw
    }

    @Test(expected = RemoteEmptyException::class)
    fun `cacheFirst_suspendLocal_online_noLocalData_remoteReturnsNull_throwsRemoteEmptyException`() = testScope.runTest {
        isOnline = true
        localData = null
        remoteData = null // Remote returns null

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        flow.first() // This should throw
    }

    @Test
    fun `cacheFirst_suspendLocal_online_localDataExists_remoteReturnsNull_emitsLocalOnly`() = testScope.runTest {
        isOnline = true
        localData = "local_initial"
        remoteData = null // Remote returns null

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertNull(cachedRemoteData) // Remote should not be cached
    }

    @Test(expected = RemoteFailedException::class)
    fun `cacheFirst_suspendLocal_online_noLocalData_remoteFails_throwsRemoteFailedException`() = testScope.runTest {
        isOnline = true
        localData = null
        val errorMessage = "Remote server error"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { throw IOException(errorMessage) },
            cacheRemote = { cachedRemoteData = it }
        )

        try {
            flow.first()
        } catch (e: RemoteFailedException) {
            assertEquals(errorMessage, e.cause?.message)
            throw e
        }
    }

    @Test
    fun `cacheFirst_suspendLocal_online_localDataExists_remoteFails_emitsLocalOnly`() = testScope.runTest {
        isOnline = true
        localData = "local_initial"
        val errorMessage = "Remote server error"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { throw IOException(errorMessage) },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertNull(cachedRemoteData) // Remote should not be cached
    }

    @Test
    fun `cacheFirst_suspendLocal_shouldFetchRemote_false_emitsLocalOnly`() = testScope.runTest {
        localData = "local_initial"
        remoteData = "remote_new"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it },
            shouldFetchRemote = { false } // Never fetch remote
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertNull(cachedRemoteData) // Remote should not be called
    }

    @Test
    fun `cacheFirst_suspendLocal_shouldEmitRemote_false_emitsLocalOnly`() = testScope.runTest {
        localData = "local_initial"
        remoteData = "remote_new"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData } as suspend () -> String?,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it },
            shouldEmitRemote = { _, _ -> false } // Never emit remote
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertEquals("remote_new", cachedRemoteData) // Remote should still be cached
    }
    //endregion

    //region dataCacheFirstInternal (Flow local) tests
    @Test
    fun `cacheFirst_flowLocal_noInitialCache_emitsRemoteAndLocalObserverUpdates`() = testScope.runTest {
        remoteData = "remote_new"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow } as () -> Flow<String?>,
            remote = { remoteData },
            cacheRemote = {
                cachedRemoteData = it
                localFlow.emit(it) // Simulate local cache update
            }
        )

        val results = mutableListOf<String>()
        val job = launch {
            flow.take(1).collect { results.add(it) } // Only take the first emission
        }
        advanceUntilIdle()

        assertEquals(listOf("remote_new"), results)
        assertEquals("remote_new", cachedRemoteData)
        job.cancel()
    }

    @Test
    fun `cacheFirst_flowLocal_initialCache_emitsInitialThenRemoteAndLocalObserverUpdates`() = testScope.runTest {
        localFlow.emit("local_initial")
        remoteData = "remote_new"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow } as () -> Flow<String?>,
            remote = { remoteData },
            cacheRemote = {
                cachedRemoteData = it
                localFlow.emit(it) // Simulate local cache update
            }
        )

        val results = mutableListOf<String>()
        val job = launch {
            flow.take(2).collect { results.add(it) } // Take initial and updated local
        }
        advanceUntilIdle()

        assertEquals(listOf("local_initial", "remote_new"), results)
        assertEquals("remote_new", cachedRemoteData)
        job.cancel()
    }

    @Test
    fun `cacheFirst_flowLocal_offline_initialCache_emitsInitialOnly`() = testScope.runTest {
        isOnline = false
        localFlow.emit("local_initial")

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow } as () -> Flow<String?>,
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertNull(cachedRemoteData) // Remote should not be called
    }

    @Test(expected = NetworkUnavailableException::class)
    fun `cacheFirst_flowLocal_offline_noInitialCache_throwsNetworkUnavailableException`() = testScope.runTest {
        isOnline = false
        localFlow.emit(null) // Ensure no initial cache

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        flow.first() // This should throw
    }

    @Test(expected = RemoteEmptyException::class)
    fun `cacheFirst_flowLocal_online_noInitialCache_remoteReturnsNull_throwsRemoteEmptyException`() = testScope.runTest {
        isOnline = true
        localFlow.emit(null)
        remoteData = null // Remote returns null

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        flow.first() // This should throw
    }

    @Test
    fun `cacheFirst_flowLocal_online_initialCache_remoteReturnsNull_emitsInitialOnly`() = testScope.runTest {
        isOnline = true
        localFlow.emit("local_initial")
        remoteData = null // Remote returns null

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertNull(cachedRemoteData) // Remote should not be cached
    }

    @Test(expected = RemoteFailedException::class)
    fun `cacheFirst_flowLocal_online_noInitialCache_remoteFails_throwsRemoteFailedException`() = testScope.runTest {
        isOnline = true
        localFlow.emit(null)
        val errorMessage = "Remote server error"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow },
            remote = { throw IOException(errorMessage) },
            cacheRemote = { cachedRemoteData = it }
        )

        try {
            flow.first()
        } catch (e: RemoteFailedException) {
            assertEquals(errorMessage, e.cause?.message)
            throw e
        }
    }

    @Test
    fun `cacheFirst_flowLocal_online_initialCache_remoteFails_emitsInitialOnly`() = testScope.runTest {
        isOnline = true
        localFlow.emit("local_initial")
        val errorMessage = "Remote server error"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow },
            remote = { throw IOException(errorMessage) },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertNull(cachedRemoteData) // Remote should not be cached
    }

    @Test
    fun `cacheFirst_flowLocal_shouldFetchRemote_false_emitsInitialOnly`() = testScope.runTest {
        localFlow.emit("local_initial")
        remoteData = "remote_new"

        val flow = dataCacheFirstInternal<String>(
            isOnline = { isOnline },
            local = { localFlow },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it },
            shouldFetchRemote = { false } // Never fetch remote
        )

        val result = flow.first()

        assertEquals("local_initial", result)
        assertNull(cachedRemoteData) // Remote should not be called
    }
    //endregion

    //region dataNetworkFirstInternal tests
    @Test
    fun `networkFirst_online_remoteSuccess_emitsRemoteAndCaches`() = testScope.runTest {
        isOnline = true
        remoteData = "remote_success"
        localData = "local_old" // Should not be used

        val flow = dataNetworkFirstInternal<String>(
            isOnline = { isOnline },
            local = { localData },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("remote_success", result)
        assertEquals("remote_success", cachedRemoteData)
    }

    @Test
    fun `networkFirst_online_remoteReturnsNull_fallsBackToLocal`() = testScope.runTest {
        isOnline = true
        remoteData = null
        localData = "local_fallback"

        val flow = dataNetworkFirstInternal<String?>(
            isOnline = { isOnline },
            local = { localData },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_fallback", result)
        assertNull(cachedRemoteData) // Remote was null, so nothing to cache
    }

    @Test
    fun `networkFirst_online_remoteReturnsNull_noLocal_emitsNull`() = testScope.runTest {
        isOnline = true
        remoteData = null
        localData = null

        val flow = dataNetworkFirstInternal<String?>(
            isOnline = { isOnline },
            local = { localData },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertNull(result)
        assertNull(cachedRemoteData)
    }

    @Test
    fun `networkFirst_online_remoteFails_fallsBackToLocal`() = testScope.runTest {
        isOnline = true
        val errorMessage = "Remote network error"
        localData = "local_fallback"

        val flow = dataNetworkFirstInternal<String?>(
            isOnline = { isOnline },
            local = { localData },
            remote = { throw IOException(errorMessage) },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_fallback", result)
        assertNull(cachedRemoteData)
    }

    @Test(expected = RemoteFailedException::class)
    fun `networkFirst_online_remoteFails_noLocal_throwsRemoteFailedException`() = testScope.runTest {
        isOnline = true
        val errorMessage = "Remote network error"
        localData = null

        val flow = dataNetworkFirstInternal<String?>(
            isOnline = { isOnline },
            local = { localData },
            remote = { throw IOException(errorMessage) },
            cacheRemote = { cachedRemoteData = it }
        )

        try {
            flow.first()
        } catch (e: RemoteFailedException) {
            assertEquals(errorMessage, e.cause?.message)
            throw e
        }
    }

    @Test
    fun `networkFirst_offline_emitsLocalOnly`() = testScope.runTest {
        isOnline = false
        localData = "local_offline"
        remoteData = "remote_online" // Should not be used

        val flow = dataNetworkFirstInternal<String?>(
            isOnline = { isOnline },
            local = { localData },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertEquals("local_offline", result)
        assertNull(cachedRemoteData) // Remote should not be called
    }

    @Test
    fun `networkFirst_offline_noLocal_emitsNull`() = testScope.runTest {
        isOnline = false
        localData = null

        val flow = dataNetworkFirstInternal<String?>(
            isOnline = { isOnline },
            local = { localData },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it }
        )

        val result = flow.first()

        assertNull(result)
        assertNull(cachedRemoteData)
    }

    @Test
    fun `networkFirst_shouldEmitLocal_false_whenRemoteNull_noEmitLocal`() = testScope.runTest {
        isOnline = true
        remoteData = null
        localData = "local_data"

        val flow = dataNetworkFirstInternal<String?>(
            isOnline = { isOnline },
            local = { localData },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it },
            shouldEmitLocal = { false } // Do not emit local data
        )

        val result = flow.first()
        assertNull(result)
    }

    @Test
    fun `networkFirst_shouldEmitLocal_true_whenRemoteNull_emitsLocal`() = testScope.runTest {
        isOnline = true
        remoteData = null
        localData = "local_data"

        val flow = dataNetworkFirstInternal<String?>(
            isOnline = { isOnline },
            local = { localData },
            remote = { remoteData },
            cacheRemote = { cachedRemoteData = it },
            shouldEmitLocal = { true } // Emit local data
        )

        val result = flow.first()
        assertEquals("local_data", result)
    }
    //endregion
}
