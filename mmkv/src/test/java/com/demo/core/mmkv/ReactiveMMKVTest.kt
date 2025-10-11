package com.demo.core.mmkv

import com.demo.core.mmkv.di.MmkvModule
import com.tencent.mmkv.MMKV
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Provides
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle

// Define a test Hilt module to provide a mocked MMKV instance
@Module
@InstallIn(SingletonComponent::class)
object TestMmkvModule {
    @Provides
    @Singleton
    fun provideMMKV(): MMKV {
        // Provide a mock MMKV instance for testing
        return mock(MMKV::class.java)
    }
}

@HiltAndroidTest
@UninstallModules(MmkvModule::class) // Uninstall the real MmkvModule
@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveMMKVTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var reactiveMMKV: ReactiveMMKV

    @Inject
    lateinit var mmkv: MMKV // Inject the mocked MMKV

    // Serializable data class for testing putObject/getObject
    @Serializable
    data class TestUser(val id: Int, val name: String)

    @Before
    fun setup() {
        hiltRule.inject()
        // Reset mock before each test
        reset(mmkv)
    }

    @After
    fun teardown() {
        // Clean up any data if necessary
        // For mock MMKV, this might not be strictly needed but good practice
    }

    @Test
    fun reactiveMMKV_is_injected() {
        // Verify that ReactiveMMKV is not null after injection
        assertNotNull(reactiveMMKV)
    }

    @Test
    fun putAndGetString_works_correctly() = runTest {
        val key = "test_string_key"
        val value = "hello world"
        val defaultValue = "default"

        // Configure mock MMKV behavior
        `when`(mmkv.encode(key, value)).thenReturn(true)
        `when`(mmkv.decodeString(key, defaultValue)).thenReturn(value)

        // Perform the put operation
        reactiveMMKV.put(key, value)

        // Verify that MMKV.encode was called
        verify(mmkv).encode(key, value)

        // Perform the get operation and assert the result
        val retrievedValue = reactiveMMKV.get(key, defaultValue)
        assertEquals(value, retrievedValue)
        verify(mmkv).decodeString(key, defaultValue)
    }

    @Test
    fun putAndGetInt_works_correctly() = runTest {
        val key = "test_int_key"
        val value = 123
        val defaultValue = 0

        `when`(mmkv.encode(key, value)).thenReturn(true)
        `when`(mmkv.decodeInt(key, defaultValue)).thenReturn(value)

        reactiveMMKV.put(key, value)
        verify(mmkv).encode(key, value)

        val retrievedValue = reactiveMMKV.get(key, defaultValue)
        assertEquals(value, retrievedValue)
        verify(mmkv).decodeInt(key, defaultValue)
    }

    @Test
    fun putAndGetObject_works_correctly() = runTest {
        val key = "test_object_key"
        val user = TestUser(1, "Alice")
        val defaultValue = TestUser(0, "Guest")
        val jsonString = Json.encodeToString(TestUser.serializer(), user)

        `when`(mmkv.encode(eq(key), anyString())).thenReturn(true)
        `when`(mmkv.decodeString(key)).thenReturn(jsonString)

        reactiveMMKV.putObject(key, user)
        verify(mmkv).encode(key, jsonString)

        val retrievedUser = reactiveMMKV.getObject(key, defaultValue)
        assertEquals(user, retrievedUser)
        verify(mmkv).decodeString(key)
    }

    @Test
    fun getFlow_emits_initial_value_and_updates() = runTest {
        val key = "flow_string_key"
        val initialValue = "initial"
        val updatedValue = "updated"
        val defaultValue = "default"

        // Configure mock MMKV for initial value
        `when`(mmkv.decodeString(key, defaultValue)).thenReturn(initialValue)

        // Collect the flow in a separate coroutine
        val flow = reactiveMMKV.getFlow(key, defaultValue)
        val emittedValues = mutableListOf<String?>()
        val job = launch {
            flow.collect { emittedValues.add(it) }
        }

        // Assert initial value
        assertEquals(listOf(initialValue), emittedValues)

        // Simulate an update
        `when`(mmkv.encode(key, updatedValue)).thenReturn(true)
        reactiveMMKV.put(key, updatedValue)

        // Allow time for flow to emit (using advanceUntilIdle for runTest)
        advanceUntilIdle()

        // Assert updated value
        assertEquals(listOf(initialValue, updatedValue), emittedValues)

        job.cancel()
    }

    @Test
    fun getObjectFlow_emits_initial_value_and_updates() = runTest {
        val key = "flow_object_key"
        val initialUser = TestUser(1, "Alice")
        val updatedUser = TestUser(2, "Bob")
        val defaultValue = TestUser(0, "Guest")

        val initialJson = Json.encodeToString(TestUser.serializer(), initialUser)
        val updatedJson = Json.encodeToString(TestUser.serializer(), updatedUser)

        // Configure mock MMKV for initial value
        `when`(mmkv.decodeString(key)).thenReturn(initialJson)

        // Collect the flow
        val flow = reactiveMMKV.getObjectFlow(key, defaultValue)
        val emittedUsers = mutableListOf<TestUser>()
        val job = launch {
            flow.collect { emittedUsers.add(it) }
        }

        // Assert initial value
        assertEquals(listOf(initialUser), emittedUsers)

        // Simulate an update
        `when`(mmkv.encode(eq(key), anyString())).thenReturn(true)
        reactiveMMKV.putObject(key, updatedUser) // This will trigger the flow update

        // Allow time for flow to emit
        advanceUntilIdle()

        // Assert updated value
        assertEquals(listOf(initialUser, updatedUser), emittedUsers)

        job.cancel()
    }

    @Test
    fun remove_key_works_correctly() = runTest {
        val key = "remove_key"
        val value = "some_value"

        `when`(mmkv.encode(key, value)).thenReturn(true)
        `when`(mmkv.decodeString(key, null)).thenReturn(value)
        doNothing().`when`(mmkv).removeValueForKey(key)

        reactiveMMKV.put(key, value)
        val retrievedValue = reactiveMMKV.get(key, null)
        assertEquals(value, retrievedValue)

        reactiveMMKV.remove(key)
        verify(mmkv).removeValueForKey(key)

        // After removal, get should return null or default.
        // We need to reconfigure mock for decodeString after removal.
        `when`(mmkv.decodeString(key, null)).thenReturn(null)
        val valueAfterRemoval = reactiveMMKV.get(key, null)
        assertNull(valueAfterRemoval)
    }
}
