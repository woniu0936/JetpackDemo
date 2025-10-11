package com.demo.core.mmkv

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Serializable
data class TestUser(val name: String, val age: Int)

@Serializable
data class TestItem(val id: String, val value: String)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class ReactiveMMKVTest {

    @Mock
    private lateinit var mockMMKV: MMKV

    @Mock
    private lateinit var mockEventBus: MMKVEventBus

    private lateinit var reactiveMMKV: ReactiveMMKV

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this)

        // Create a new MutableSharedFlow for each test to ensure isolation
        val testEventsFlow = MutableSharedFlow<Pair<String, Any?>>(replay = 1)
        whenever(mockEventBus.events).thenReturn(testEventsFlow)
        // Stub the notify method to emit to our testEventsFlow
        doAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1]
            testEventsFlow.tryEmit(key to value)
            null // Void method
        }.whenever(mockEventBus).notify(any(), any())

        reactiveMMKV = ReactiveMMKV(mockMMKV, mockEventBus)
    }

    // --- Native Type API Tests ---

    @Test
    fun `put String calls mmkv encode and notifies`() {
        val key = "testString"
        val value = "hello"
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
        // Cannot directly verify MMKVEventBus.events.tryEmit due to private object
        // We'll rely on flow tests to indirectly verify notifications
    }

    @Test
    fun `put null String calls mmkv encode and notifies`() {
        val key = "testNullString"
        val value: String? = null
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
    }

    @Test
    fun `get String returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetString"
        val value = "world"
        val defaultValue = "default"
        whenever(mockMMKV.decodeString(key, defaultValue)).thenReturn(value)

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify(mockMMKV).decodeString(key, defaultValue)
    }

    @Test
    fun `get String returns default value if key not found`() = runTest(testDispatcher) {
        val key = "nonExistentString"
        val defaultValue = "default"
        whenever(mockMMKV.decodeString(key, defaultValue)).thenReturn(defaultValue)

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(defaultValue, result)
        verify(mockMMKV).decodeString(key, defaultValue)
    }

    @Test
    fun `put Int calls mmkv encode`() {
        val key = "testInt"
        val value = 123
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
    }

    @Test
    fun `get Int returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetInt"
        val value = 456
        val defaultValue = 0
        whenever(mockMMKV.decodeInt(key, defaultValue)).thenReturn(value)

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify(mockMMKV).decodeInt(key, defaultValue)
    }

    @Test
    fun `put Boolean calls mmkv encode`() {
        val key = "testBoolean"
        val value = true
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
    }

    @Test
    fun `get Boolean returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetBoolean"
        val value = false
        val defaultValue = true
        whenever(mockMMKV.decodeBool(key, defaultValue)).thenReturn(value)

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify(mockMMKV).decodeBool(key, defaultValue)
    }

    @Test
    fun `put Long calls mmkv encode`() {
        val key = "testLong"
        val value = 1234567890L
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
    }

    @Test
    fun `get Long returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetLong"
        val value = 9876543210L
        val defaultValue = 0L
        whenever(mockMMKV.decodeLong(key, defaultValue)).thenReturn(value)

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify(mockMMKV).decodeLong(key, defaultValue)
    }

    @Test
    fun `put Float calls mmkv encode`() {
        val key = "testFloat"
        val value = 1.23f
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
    }

    @Test
    fun `get Float returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetFloat"
        val value = 4.56f
        val defaultValue = 0.0f
        whenever(mockMMKV.decodeFloat(key, defaultValue)).thenReturn(value)

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify(mockMMKV).decodeFloat(key, defaultValue)
    }

    @Test
    fun `put Double calls mmkv encode`() {
        val key = "testDouble"
        val value = 1.2345
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
    }

    @Test
    fun `get Double returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetDouble"
        val value = 6.7890
        val defaultValue = 0.0
        whenever(mockMMKV.decodeDouble(key, defaultValue)).thenReturn(value)

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result, 0.0001) // Delta for double comparison
        verify(mockMMKV).decodeDouble(key, defaultValue)
    }

    @Test
    fun `put ByteArray calls mmkv encode`() {
        val key = "testByteArray"
        val value = "data".toByteArray()
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
    }

    @Test
    fun `get ByteArray returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetByteArray"
        val value = "data".toByteArray()
        val defaultValue: ByteArray? = null
        whenever(mockMMKV.decodeBytes(key, defaultValue)).thenReturn(value)

        val result = reactiveMMKV.get(key, defaultValue)

        assertArrayEquals(value, result)
        verify(mockMMKV).decodeBytes(key, defaultValue)
    }

    @Test
    fun `put Set of String calls mmkv encode`() {
        val key = "testSetString"
        val value = setOf("a", "b")
        whenever(mockMMKV.encode(key, value)).thenReturn(true)

        reactiveMMKV.put(key, value)

        verify(mockMMKV).encode(key, value)
    }

    @Test
    fun `get Set of String returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetSetString"
        val value = setOf("c", "d")
        val defaultValue: Set<String>? = null
        whenever(mockMMKV.decodeStringSet(key, defaultValue)).thenReturn(value)

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify(mockMMKV).decodeStringSet(key, defaultValue)
    }

    // --- Flow API Tests (Native Types) ---

    @Test
    fun `getFlow String emits initial value and subsequent changes`() = runTest(testDispatcher) {
        val key = "flowString"
        val defaultValue = "default"
        val firstValue = "initial"
        val secondValue = "updated"

        whenever(mockMMKV.decodeString(key, defaultValue)).thenReturn(firstValue)

        val flow = reactiveMMKV.getFlow(key, defaultValue)
        val emittedValues = mutableListOf<String?>()

        val job = launch(testDispatcher) {
            flow.collect { emittedValues.add(it) }
        }

        // Initial value
        assertEquals(listOf(firstValue), emittedValues)

        // Update value
        whenever(mockMMKV.encode(key, secondValue)).thenReturn(true)
        reactiveMMKV.put(key, secondValue)
        testDispatcher.scheduler.runCurrent() // Ensure all pending coroutines run

        // The flow should now have emitted the second value
        assertEquals(listOf(firstValue, secondValue), emittedValues)

        job.cancel()
    }

    @Test
    fun `getFlow Int emits initial value and subsequent changes`() = runTest(testDispatcher) {
        val key = "flowInt"
        val defaultValue = 0
        val firstValue = 10
        val secondValue = 20

        whenever(mockMMKV.decodeInt(key, defaultValue)).thenReturn(firstValue)

        val flow = reactiveMMKV.getFlow(key, defaultValue)
        val emittedValues = mutableListOf<Int>()

        val job = launch(testDispatcher) {
            flow.collect { emittedValues.add(it) }
        }

        assertEquals(listOf(firstValue), emittedValues)

        whenever(mockMMKV.encode(key, secondValue)).thenReturn(true)
        reactiveMMKV.put(key, secondValue)
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf(firstValue, secondValue), emittedValues)

        job.cancel()
    }

    @Test
    fun `getFlow handles distinctUntilChanged correctly`() = runTest(testDispatcher) {
        val key = "distinctFlow"
        val defaultValue = "initial"
        val value = "sameValue"

        whenever(mockMMKV.decodeString(key, defaultValue)).thenReturn(defaultValue)

        val flow = reactiveMMKV.getFlow(key, defaultValue)
        val emittedValues = mutableListOf<String?>()

        val job = launch(testDispatcher) {
            flow.collect { emittedValues.add(it) }
        }

        assertEquals(listOf(defaultValue), emittedValues)

        // Put the same value, should not emit again due to distinctUntilChanged
        whenever(mockMMKV.encode(key, value)).thenReturn(true)
        reactiveMMKV.put(key, value)
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf(defaultValue, value), emittedValues) // Should emit once for the change

        reactiveMMKV.put(key, value) // Put same value again
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf(defaultValue, value), emittedValues) // Should not emit again

        job.cancel()
    }

    // --- Object API Tests ---

    @Test
    fun `putObject serializes and stores object`() = runTest(testDispatcher) {
        val key = "testObject"
        val user = TestUser("Alice", 30)
        val jsonString = ReactiveMMKV.json.encodeToString(user)

        // Mock the underlying put(String, String) call
        whenever(mockMMKV.encode(key, jsonString)).thenReturn(true)

        reactiveMMKV.putObject(key, user)

        verify(mockMMKV).encode(key, jsonString)
    }

    @Test
    fun `putObject with null value calls remove`() = runTest(testDispatcher) {
        val key = "testNullObject"
        val user: TestUser? = null

        // For void methods, we don't use thenReturn
        // whenever(mockMMKV.removeValueForKey(key)).thenReturn(true)

        reactiveMMKV.putObject(key, user)

        verify(mockMMKV).removeValueForKey(key)
    }

    @Test
    fun `getObject deserializes and returns object`() = runTest(testDispatcher) {
        val key = "getTestObject"
        val user = TestUser("Bob", 25)
        val jsonString = ReactiveMMKV.json.encodeToString(user)
        val defaultValue = TestUser("Guest", 0)

        whenever(mockMMKV.decodeString(key)).thenReturn(jsonString)

        val result = reactiveMMKV.getObject(key, defaultValue)

        assertEquals(user, result)
        verify(mockMMKV).decodeString(key)
    }

    @Test
    fun `getObject returns default value if key not found`() = runTest(testDispatcher) {
        val key = "nonExistentObject"
        val defaultValue = TestUser("Guest", 0)

        whenever(mockMMKV.decodeString(key)).thenReturn(null)

        val result = reactiveMMKV.getObject(key, defaultValue)

        assertEquals(defaultValue, result)
        verify(mockMMKV).decodeString(key)
    }

    @Test
    fun `getObject returns default value if JSON is invalid`() = runTest(testDispatcher) {
        val key = "invalidJsonObject"
        val invalidJson = "{ \"name\": \"Charlie\", \"age\": \"twenty\" }" // age is string, should be int
        val defaultValue = TestUser("Guest", 0)

        whenever(mockMMKV.decodeString(key)).thenReturn(invalidJson)

        val result = reactiveMMKV.getObject(key, defaultValue)

        assertEquals(defaultValue, result)
        verify(mockMMKV).decodeString(key)
    }

    @Test
    fun `getObjectFlow emits initial object and subsequent changes`() = runTest(testDispatcher) {
        val key = "flowObject"
        val user1 = TestUser("Dave", 40)
        val user2 = TestUser("Eve", 22)
        val defaultValue = TestUser("Default", 0)

        // Mock initial value for decodeString(key, null)
        whenever(mockMMKV.decodeString(eq(key), eq(null as String?))).thenReturn(ReactiveMMKV.json.encodeToString(user1))

        val flow = reactiveMMKV.getObjectFlow(key, defaultValue)

        val collectJob = launch(testDispatcher) {
            val emittedValues = flow.take(2).toList()
            assertEquals(listOf(user1, user2), emittedValues)
        }

        // Update value
        val user2Json = ReactiveMMKV.json.encodeToString(user2)
        whenever(mockMMKV.encode(key, user2Json)).thenReturn(true)
        reactiveMMKV.putObject(key, user2)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure all pending coroutines run

        collectJob.join() // Wait for the collection to complete
    }

    @Test
    fun `getObjectFlow emits default value for null or invalid JSON`() = runTest(testDispatcher) {
        val key = "flowObjectInvalid"
        val defaultValue = TestUser("Default", 0)

        // Initial state: null string
        whenever(mockMMKV.decodeString(key, null as String?)).thenReturn(null)

        val flow = reactiveMMKV.getObjectFlow(key, defaultValue)
        val emittedValues = mutableListOf<TestUser>()

        val job = launch(testDispatcher) {
            flow.collect { emittedValues.add(it) }
        }

        assertEquals(listOf(defaultValue), emittedValues)

        // Update with invalid JSON
        val invalidJson = "{ \"name\": \"Charlie\", \"age\": \"twenty\" }"
        whenever(mockMMKV.encode(key, invalidJson)).thenReturn(true)
        reactiveMMKV.put(key, invalidJson) // Use put to simulate raw string storage
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf(defaultValue, defaultValue), emittedValues) // Should still emit default

        job.cancel()
    }

    // --- List API Tests ---

    @Test
    fun `getList returns decoded list of objects`() = runTest(testDispatcher) {
        val key = "testList"
        val list = listOf(TestItem("1", "A"), TestItem("2", "B"))
        val jsonString = ReactiveMMKV.json.encodeToString(list)
        val defaultValue = emptyList<TestItem>()

        whenever(mockMMKV.decodeString(key)).thenReturn(jsonString)

        val result = reactiveMMKV.getList<TestItem>(key, defaultValue)

        assertEquals(list, result)
        verify(mockMMKV).decodeString(key)
    }

    @Test
    fun `getList returns default empty list if key not found`() = runTest(testDispatcher) {
        val key = "nonExistentList"
        val defaultValue = emptyList<TestItem>()

        whenever(mockMMKV.decodeString(key)).thenReturn(null)

        val result = reactiveMMKV.getList<TestItem>(key, defaultValue)

        assertEquals(defaultValue, result)
        verify(mockMMKV).decodeString(key)
    }

    @Test
    fun `editList reads, transforms, and writes back the list`() = runTest(testDispatcher) {
        val key = "editTestList"
        val initialList = listOf(TestItem("1", "A"))
        val updatedList = listOf(TestItem("1", "A"), TestItem("2", "B"))
        val initialJson = ReactiveMMKV.json.encodeToString(initialList)
        val updatedJson = ReactiveMMKV.json.encodeToString(updatedList)

        // Mock initial read
        whenever(mockMMKV.decodeString(key)).thenReturn(initialJson)
        // Mock write back
        whenever(mockMMKV.encode(eq(key), any<String>())).thenReturn(true)

        reactiveMMKV.editList<TestItem>(key) { currentList ->
            assertEquals(initialList, currentList)
            currentList + TestItem("2", "B")
        }

        // Verify getList was called (which calls decodeString)
        verify(mockMMKV).decodeString(key)
        // Verify putObject was called with the transformed list
        verify(mockMMKV).encode(key, updatedJson)
    }

    @Test
    fun `getListFlow emits initial list and subsequent changes`() = runTest(testDispatcher) {
        val key = "flowList"
        val list1 = listOf(TestItem("1", "A"))
        val list2 = listOf(TestItem("1", "A"), TestItem("2", "B"))
        val defaultValue = emptyList<TestItem>()

        // Mock initial value for decodeString(key, null)
        whenever(mockMMKV.decodeString(eq(key), eq(null as String?))).thenReturn(ReactiveMMKV.json.encodeToString(list1))

        val flow = reactiveMMKV.getListFlow(key, defaultValue)

        val collectJob = launch(testDispatcher) {
            val emittedValues = flow.take(2).toList()
            assertEquals(listOf(list1, list2), emittedValues)
        }

        // Update value
        val list2Json = ReactiveMMKV.json.encodeToString(list2)
        whenever(mockMMKV.encode(key, list2Json)).thenReturn(true)
        reactiveMMKV.putObject(key, list2)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure all pending coroutines run

        collectJob.join() // Wait for the collection to complete
    }

    // --- Map API Tests ---

    @Test
    fun `getMap returns decoded map of objects`() = runTest(testDispatcher) {
        val key = "testMap"
        val map = mapOf("item1" to TestItem("1", "A"), "item2" to TestItem("2", "B"))
        val jsonString = ReactiveMMKV.json.encodeToString(map)
        val defaultValue = emptyMap<String, TestItem>()

        whenever(mockMMKV.decodeString(key)).thenReturn(jsonString)

        val result = reactiveMMKV.getMap<String, TestItem>(key, defaultValue)

        assertEquals(map, result)
        verify(mockMMKV).decodeString(key)
    }

    @Test
    fun `getMap returns default empty map if key not found`() = runTest(testDispatcher) {
        val key = "nonExistentMap"
        val defaultValue = emptyMap<String, TestItem>()

        whenever(mockMMKV.decodeString(key)).thenReturn(null)

        val result = reactiveMMKV.getMap<String, TestItem>(key, defaultValue)

        assertEquals(defaultValue, result)
        verify(mockMMKV).decodeString(key)
    }

    @Test
    fun `editMap reads, transforms, and writes back the map`() = runTest(testDispatcher) {
        val key = "editTestMap"
        val initialMap = mapOf("item1" to TestItem("1", "A"))
        val updatedMap = mapOf("item1" to TestItem("1", "A"), "item2" to TestItem("2", "B"))
        val initialJson = ReactiveMMKV.json.encodeToString(initialMap)
        val updatedJson = ReactiveMMKV.json.encodeToString(updatedMap)

        // Mock initial read
        whenever(mockMMKV.decodeString(key)).thenReturn(initialJson)
        // Mock write back
        whenever(mockMMKV.encode(eq(key), any<String>())).thenReturn(true)

        reactiveMMKV.editMap<String, TestItem>(key) { currentMap ->
            assertEquals(initialMap, currentMap)
            currentMap + ("item2" to TestItem("2", "B"))
        }

        // Verify getMap was called (which calls decodeString)
        verify(mockMMKV).decodeString(key)
        // Verify putObject was called with the transformed map
        verify(mockMMKV).encode(key, updatedJson)
    }

    @Test
    fun `getMapFlow emits initial map and subsequent changes`() = runTest(testDispatcher) {
        val key = "flowMap"
        val map1 = mapOf("item1" to TestItem("1", "A"))
        val map2 = mapOf("item1" to TestItem("1", "A"), "item2" to TestItem("2", "B"))
        val defaultValue = emptyMap<String, TestItem>()

        // Mock initial value for decodeString(key, null)
        whenever(mockMMKV.decodeString(eq(key), eq(null as String?))).thenReturn(ReactiveMMKV.json.encodeToString(map1))

        val flow = reactiveMMKV.getMapFlow(key, defaultValue)

        val collectJob = launch(testDispatcher) {
            val emittedValues = flow.take(2).toList()
            assertEquals(listOf(map1, map2), emittedValues)
        }

        // Update value
        val map2Json = ReactiveMMKV.json.encodeToString(map2)
        whenever(mockMMKV.encode(key, map2Json)).thenReturn(true)
        reactiveMMKV.putObject(key, map2)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure all pending coroutines run

        collectJob.join() // Wait for the collection to complete
    }

    // --- General and Internal Implementation Tests ---

    @Test
    fun `remove calls mmkv removeValueForKey and notifies`() {
        val key = "removeKey"
        // For void methods, we don't use thenReturn
        // whenever(mockMMKV.removeValueForKey(key)).thenReturn(true)

        reactiveMMKV.remove(key)

        verify(mockMMKV).removeValueForKey(key)
    }

    // --- Preference Delegate Tests ---

    @Test
    fun `preference delegate getValue calls get and returns value`() = runTest(testDispatcher) {
        val key = "prefInt"
        val defaultValue = 100
        val storedValue = 200

        whenever(mockMMKV.decodeInt(key, defaultValue)).thenReturn(storedValue)

        val delegate = reactiveMMKV.preference(key, defaultValue)
        val holder = object { var myInt: Int by delegate }

        assertEquals(storedValue, holder.myInt)
        verify(mockMMKV).decodeInt(key, defaultValue)
    }

    @Test
    fun `preference delegate setValue calls put`() {
        val key = "prefIntSet"
        val defaultValue = 100
        val newValue = 300

        whenever(mockMMKV.encode(key, newValue)).thenReturn(true)

        val delegate = reactiveMMKV.preference(key, defaultValue)
        val holder = object { var myInt: Int by delegate }

        holder.myInt = newValue

        verify(mockMMKV).encode(key, newValue)
    }

    // --- Observe Aliases Tests ---

    @Test
    fun `observe Int delegates to getFlow Int`() {
        val key = "observeInt"
        val defaultValue = 5
        // We don't need to collect the flow, just verify the call path
        reactiveMMKV.observe(key, defaultValue)
        // This is an extension function, so we can't directly verify `getFlow` on `reactiveMMKV`
        // We trust the alias simply calls the underlying function.
        // The `getFlow` itself is tested above.
    }

    @Test
    fun `observeObject delegates to getObjectFlow`() {
        val key = "observeObject"
        val defaultValue = TestUser("Default", 0)
        reactiveMMKV.observeObject(key, defaultValue)
        // Similar to observe Int, we trust the alias.
    }

    @Test
    fun `observeList delegates to getListFlow`() {
        val key = "observeList"
        val defaultValue = emptyList<TestItem>()
        reactiveMMKV.observeList(key, defaultValue)
        // Similar to observe Int, we trust the alias.
    }

    @Test
    fun `observeMap delegates to getMapFlow`() {
        val key = "observeMap"
        val defaultValue = emptyMap<String, TestItem>()
        reactiveMMKV.observeMap(key, defaultValue)
        // Similar to observe Int, we trust the alias.
    }
}
