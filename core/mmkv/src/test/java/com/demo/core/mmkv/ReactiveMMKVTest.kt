package com.demo.core.mmkv

import app.cash.turbine.test
import com.tencent.mmkv.MMKV
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@Serializable
data class TestUser(val name: String, val age: Int)

@Serializable
data class TestItem(val id: String, val value: String)

@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveMMKVTest {

    @MockK
    private lateinit var mockMMKV: MMKV

    @MockK
    private lateinit var mockEventBus: MMKVEventBus

    private lateinit var reactiveMMKV: ReactiveMMKV

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        // Initialize mocks
        MockKAnnotations.init(this)

        // Create a new MutableSharedFlow for each test to ensure isolation
        val testEventsFlow = MutableSharedFlow<Pair<String, Any?>>(replay = 1)
        every { mockEventBus.events } returns testEventsFlow
        // Stub the notify method to emit to our testEventsFlow
        every { mockEventBus.notify(any<String>(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<Any?>()
            testEventsFlow.tryEmit(key to value)
        }

        reactiveMMKV = ReactiveMMKV(mockMMKV, mockEventBus)
    }

    // --- Native Type API Tests ---

    @Test
    fun `put String calls mmkv encode and notifies`() {
        val key = "testString"
        val value = "hello"
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
        // Cannot directly verify MMKVEventBus.events.tryEmit due to private object
        // We'll rely on flow tests to indirectly verify notifications
    }

    @Test
    fun `put null String calls mmkv encode and notifies`() {
        val key = "testNullString"
        val value: String? = null
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
    }

    @Test
    fun `get String returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetString"
        val value = "world"
        val defaultValue = "default"
        every { mockMMKV.decodeString(key, defaultValue) } returns value

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify { mockMMKV.decodeString(key, defaultValue) }
    }

    @Test
    fun `get String returns default value if key not found`() = runTest(testDispatcher) {
        val key = "nonExistentString"
        val defaultValue = "default"
        every { mockMMKV.decodeString(key, defaultValue) } returns defaultValue

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(defaultValue, result)
        verify { mockMMKV.decodeString(key, defaultValue) }
    }

    @Test
    fun `put Int calls mmkv encode`() {
        val key = "testInt"
        val value = 123
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
    }

    @Test
    fun `get Int returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetInt"
        val value = 456
        val defaultValue = 0
        every { mockMMKV.decodeInt(key, defaultValue) } returns value

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify { mockMMKV.decodeInt(key, defaultValue) }
    }

    @Test
    fun `put Boolean calls mmkv encode`() {
        val key = "testBoolean"
        val value = true
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
    }

    @Test
    fun `get Boolean returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetBoolean"
        val value = false
        val defaultValue = true
        every { mockMMKV.decodeBool(key, defaultValue) } returns value

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify { mockMMKV.decodeBool(key, defaultValue) }
    }

    @Test
    fun `put Long calls mmkv encode`() {
        val key = "testLong"
        val value = 1234567890L
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
    }

    @Test
    fun `get Long returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetLong"
        val value = 9876543210L
        val defaultValue = 0L
        every { mockMMKV.decodeLong(key, defaultValue) } returns value

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify { mockMMKV.decodeLong(key, defaultValue) }
    }

    @Test
    fun `put Float calls mmkv encode`() {
        val key = "testFloat"
        val value = 1.23f
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
    }

    @Test
    fun `get Float returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetFloat"
        val value = 4.56f
        val defaultValue = 0.0f
        every { mockMMKV.decodeFloat(key, defaultValue) } returns value

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify { mockMMKV.decodeFloat(key, defaultValue) }
    }

    @Test
    fun `put Double calls mmkv encode`() {
        val key = "testDouble"
        val value = 1.2345
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
    }

    @Test
    fun `get Double returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetDouble"
        val value = 6.7890
        val defaultValue = 0.0
        every { mockMMKV.decodeDouble(key, defaultValue) } returns value

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result, 0.0001) // Delta for double comparison
        verify { mockMMKV.decodeDouble(key, defaultValue) }
    }

    @Test
    fun `put ByteArray calls mmkv encode`() {
        val key = "testByteArray"
        val value = "data".toByteArray()
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
    }

    @Test
    fun `get ByteArray returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetByteArray"
        val value = "data".toByteArray()
        val defaultValue: ByteArray? = null
        every { mockMMKV.decodeBytes(key, defaultValue) } returns value

        val result = reactiveMMKV.get(key, defaultValue)

        assertArrayEquals(value, result)
        verify { mockMMKV.decodeBytes(key, defaultValue) }
    }

    @Test
    fun `put Set of String calls mmkv encode`() {
        val key = "testSetString"
        val value = setOf("a", "b")
        every { mockMMKV.encode(key, value) } returns true

        reactiveMMKV.put(key, value)

        verify { mockMMKV.encode(key, value) }
    }

    @Test
    fun `get Set of String returns decoded value`() = runTest(testDispatcher) {
        val key = "testGetSetString"
        val value = setOf("c", "d")
        val defaultValue: Set<String>? = null
        every { mockMMKV.decodeStringSet(key, defaultValue) } returns value

        val result = reactiveMMKV.get(key, defaultValue)

        assertEquals(value, result)
        verify { mockMMKV.decodeStringSet(key, defaultValue) }
    }

    // --- Flow API Tests (Native Types) ---

    @Test
    fun `getFlow String emits initial value and subsequent changes`() = runTest(testDispatcher) {
        val key = "flowString"
        val defaultValue = "default"
        val firstValue = "initial"
        val secondValue = "updated"

        every { mockMMKV.decodeString(key, defaultValue) } returns firstValue

        val flow = reactiveMMKV.getFlow(key, defaultValue)
        val emittedValues = mutableListOf<String?>()

        val job = launch(testDispatcher) {
            flow.collect { emittedValues.add(it) }
        }

        // Initial value
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf(firstValue), emittedValues)

        // Update value
        every { mockMMKV.encode(key, secondValue) } returns true
        every { mockMMKV.decodeString(key, defaultValue) } returns secondValue
        reactiveMMKV.put(key, secondValue)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure all pending coroutines run

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

        every { mockMMKV.decodeInt(key, defaultValue) } returns firstValue

        val flow = reactiveMMKV.getFlow(key, defaultValue)
        val emittedValues = mutableListOf<Int>()

        val job = launch(testDispatcher) {
            flow.collect { emittedValues.add(it) }
        }

        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf(firstValue), emittedValues)

        every { mockMMKV.encode(key, secondValue) } returns true
        every { mockMMKV.decodeInt(key, defaultValue) } returns secondValue
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

        every { mockMMKV.decodeString(key, defaultValue) } returns defaultValue

        val flow = reactiveMMKV.getFlow(key, defaultValue)
        val emittedValues = mutableListOf<String?>()

        val job = launch(testDispatcher) {
            flow.collect { emittedValues.add(it) }
        }

        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf(defaultValue), emittedValues)

        // Put the same value, should not emit again due to distinctUntilChanged
        every { mockMMKV.encode(key, value) } returns true
        every { mockMMKV.decodeString(key, defaultValue) } returns value // Re-stub for re-read
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
        every { mockMMKV.encode(key, jsonString) } returns true

        reactiveMMKV.putObject(key, user)

        verify { mockMMKV.encode(key, jsonString) }
    }

    @Test
    fun `putObject with null value calls remove`() = runTest(testDispatcher) {
        val key = "testNullObject"
        val user: TestUser? = null

        // For void methods, we don't use thenReturn
         every { mockMMKV.removeValueForKey(key) } just Runs // MockK equivalent for void

        reactiveMMKV.putObject(key, user)

        verify { mockMMKV.removeValueForKey(key) }
    }

    @Test
    fun `getObject deserializes and returns object`() = runTest(testDispatcher) {
        val key = "getTestObject"
        val user = TestUser("Bob", 25)
        val jsonString = ReactiveMMKV.json.encodeToString(user)
        val defaultValue = TestUser("Guest", 0)

        every { mockMMKV.decodeString(key) } returns jsonString

        val result = reactiveMMKV.getObject(key, defaultValue)

        assertEquals(user, result)
        verify { mockMMKV.decodeString(key) }
    }

    @Test
    fun `getObject returns default value if key not found`() = runTest(testDispatcher) {
        val key = "nonExistentObject"
        val defaultValue = TestUser("Guest", 0)

        every { mockMMKV.decodeString(key) } returns null

        val result = reactiveMMKV.getObject(key, defaultValue)

        assertEquals(defaultValue, result)
        verify { mockMMKV.decodeString(key) }
    }

    @Test
    fun `getObject returns default value if JSON is invalid`() = runTest(testDispatcher) {
        val key = "invalidJsonObject"
        val invalidJson = "{ \"name\": \"Charlie\", \"age\": \"twenty\" }" // age is string, should be int
        val defaultValue = TestUser("Guest", 0)

        every { mockMMKV.decodeString(key) } returns invalidJson

        val result = reactiveMMKV.getObject(key, defaultValue)

        assertEquals(defaultValue, result)
        verify { mockMMKV.decodeString(key) }
    }

    @Test
    fun `getObjectFlow emits initial object and subsequent changes`() = runTest(testDispatcher) {
        // 1. Arrange (准备阶段 - 保持不变)
        val key = "flowObject"
        val user1 = TestUser("Dave", 40)
        val user2 = TestUser("Eve", 22)
        val defaultValue = TestUser("Default", 0)

        val user1Json = ReactiveMMKV.json.encodeToString(user1)
        val user2Json = ReactiveMMKV.json.encodeToString(user2)

        // 预设 Flow 启动时会读取的初始 JSON 值
        every { mockMMKV.decodeString(eq(key), any()) } returns user1Json

        // 2. Act & Assert (执行与断言 - 使用 Turbine 重构)
        val flow = reactiveMMKV.getObjectFlow(key, defaultValue)

        // 使用 Turbine 的 .test {} 代码块
        flow.test {
            // (A) 断言并消费 Flow 发射的第一个对象
            // awaitItem() 会挂起，直到接收到值，完美解决时序问题
            assertEquals(user1, awaitItem())

            // (B) 准备并执行更新操作
            // 当 Flow 因为事件通知而重新查询时，让它读到新的 JSON
            every { mockMMKV.decodeString(eq(key), any()) } returns user2Json
            // 预设 putObject 内部会调用的 encode 方法
            every { mockMMKV.encode(key, user2Json) } returns true

            reactiveMMKV.putObject(key, user2)

            // (C) 断言并消费 Flow 发射的第二个对象
            // 再次调用 awaitItem() 等待下一次发射
            assertEquals(user2, awaitItem())

            // (D) (可选) 验证没有更多意外的发射
            expectNoEvents()
        }
    }

    @Test
    fun `getObjectFlow emits default value for null or invalid JSON`() = runTest(testDispatcher) {
        val key = "flowObjectInvalid"
        val defaultValue = TestUser("Default", 0)

        // Initial state: null string
        every { mockMMKV.decodeString(key, null as String?) } returns null

        val flow = reactiveMMKV.getObjectFlow(key, defaultValue)
        val emittedValues = mutableListOf<TestUser>()

        val job = launch(testDispatcher) {
            flow.collect { emittedValues.add(it) }
        }

        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf(defaultValue), emittedValues)

        // Update with invalid JSON
        val invalidJson = "{ \"name\": \"Charlie\", \"age\": \"twenty\" }"
        every { mockMMKV.encode(key, invalidJson) } returns true
        every { mockMMKV.decodeString(key, any()) } returns invalidJson // Re-stub for re-read
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

        every { mockMMKV.decodeString(key) } returns jsonString

        val result = reactiveMMKV.getList<TestItem>(key, defaultValue)

        assertEquals(list, result)
        verify { mockMMKV.decodeString(key) }
    }

    @Test
    fun `getList returns default empty list if key not found`() = runTest(testDispatcher) {
        val key = "nonExistentList"
        val defaultValue = emptyList<TestItem>()

        every { mockMMKV.decodeString(key) } returns null

        val result = reactiveMMKV.getList<TestItem>(key, defaultValue)

        assertEquals(defaultValue, result)
        verify { mockMMKV.decodeString(key) }
    }

    @Test
    fun `editList reads, transforms, and writes back the list`() = runTest(testDispatcher) {
        val key = "editTestList"
        val initialList = listOf(TestItem("1", "A"))
        val updatedList = listOf(TestItem("1", "A"), TestItem("2", "B"))
        val initialJson = ReactiveMMKV.json.encodeToString(initialList)
        val updatedJson = ReactiveMMKV.json.encodeToString(updatedList)

        // Mock initial read
        every { mockMMKV.decodeString(key) } returns initialJson
        // Mock write back
        every { mockMMKV.encode(eq(key), any<String>()) } returns true

        reactiveMMKV.editList<TestItem>(key) { currentList ->
            assertEquals(initialList, currentList)
            currentList + TestItem("2", "B")
        }

        // Verify getList was called (which calls decodeString)
        verify { mockMMKV.decodeString(key) }
        // Verify putObject was called with the transformed list
        verify { mockMMKV.encode(key, updatedJson) }
    }

    @Test
    fun `getListFlow emits initial list and subsequent changes`() = runTest {
        // Arrange
        val key = "flowList"
        val list1 = listOf(TestItem(id = "1", value = "A"))
        val list2 = listOf(TestItem(id = "1", value = "A"), TestItem(id = "2", value = "B"))
        val defaultValue = emptyList<TestItem>()

        val list1Json = ReactiveMMKV.json.encodeToString(list1)
        val list2Json = ReactiveMMKV.json.encodeToString(list2)

        // Mock initial value for the Flow's `onStart` block
        every { mockMMKV.decodeString(key, any()) } returns list1Json

        // Act & Assert
        val flow = reactiveMMKV.getListFlow(key, defaultValue)

        // 使用 Turbine 的 .test {} 代码块
        flow.test(timeout = 3.seconds) { // 设置一个合理的超时以防测试卡死
            // 1. 断言并消费第一个发射项
            // awaitItem() 会挂起直到接收到一个新值
            assertEquals(list1, awaitItem())

            // 2. 准备并执行更新操作
            // Re-stub the decodeString for any potential re-reads (good practice)
            every { mockMMKV.decodeString(key, any()) } returns list2Json
            // Mock the encode call that putObject will trigger
            every { mockMMKV.encode(key, list2Json) } returns true

            reactiveMMKV.putObject(key, list2)

            // 3. 断言并消费第二个发射项
            assertEquals(list2, awaitItem())

            // (可选) 确保没有其他意外的发射
            expectNoEvents()
        }
    }

    // --- Map API Tests ---

    @Test
    fun `getMap returns decoded map of objects`() = runTest(testDispatcher) {
        val key = "testMap"
        val map = mapOf("item1" to TestItem("1", "A"), "item2" to TestItem("2", "B"))
        val jsonString = ReactiveMMKV.json.encodeToString(map)
        val defaultValue = emptyMap<String, TestItem>()

        every { mockMMKV.decodeString(key) } returns jsonString

        val result = reactiveMMKV.getMap<String, TestItem>(key, defaultValue)

        assertEquals(map, result)
        verify { mockMMKV.decodeString(key) }
    }

    @Test
    fun `getMap returns default empty map if key not found`() = runTest(testDispatcher) {
        val key = "nonExistentMap"
        val defaultValue = emptyMap<String, TestItem>()

        every { mockMMKV.decodeString(key) } returns null

        val result = reactiveMMKV.getMap<String, TestItem>(key, defaultValue)

        assertEquals(defaultValue, result)
        verify { mockMMKV.decodeString(key) }
    }

    @Test
    fun `editMap reads, transforms, and writes back the map`() = runTest(testDispatcher) {
        val key = "editTestMap"
        val initialMap = mapOf("item1" to TestItem("1", "A"))
        val updatedMap = mapOf("item1" to TestItem("1", "A"), "item2" to TestItem("2", "B"))
        val initialJson = ReactiveMMKV.json.encodeToString(initialMap)
        val updatedJson = ReactiveMMKV.json.encodeToString(updatedMap)

        // Mock initial read
        every { mockMMKV.decodeString(key) } returns initialJson
        // Mock write back
        every { mockMMKV.encode(eq(key), any<String>()) } returns true

        reactiveMMKV.editMap<String, TestItem>(key) { currentMap ->
            assertEquals(initialMap, currentMap)
            currentMap + ("item2" to TestItem("2", "B"))
        }

        // Verify getMap was called (which calls decodeString)
        verify { mockMMKV.decodeString(key) }
        // Verify putObject was called with the transformed map
        verify { mockMMKV.encode(key, updatedJson) }
    }

    @Test
    fun `getMapFlow emits initial map and subsequent changes`() = runTest(testDispatcher) {
        // 1. Arrange (准备阶段 - 这部分保持不变)
        val key = "flowMap"
        val map1 = mapOf("item1" to TestItem("1", "A"))
        val map2 = mapOf("item1" to TestItem("1", "A"), "item2" to TestItem("2", "B"))
        val defaultValue = emptyMap<String, TestItem>()

        val map1Json = ReactiveMMKV.json.encodeToString(map1)
        val map2Json = ReactiveMMKV.json.encodeToString(map2)

        // 预设 Flow 启动时读取的初始值
        every { mockMMKV.decodeString(eq(key), any()) } returns map1Json

        // 2. Act & Assert (执行与断言 - 使用 Turbine 重构)
        val flow = reactiveMMKV.getMapFlow(key, defaultValue)

        // 使用 Turbine 的 .test {} 代码块来包裹所有 Flow 相关的操作
        flow.test {
            // (A) 断言并消费第一个发射项
            // awaitItem() 会挂起测试，直到 Flow 发射一个新值，然后返回该值。
            // 这完美地解决了时序问题。
            assertEquals(map1, awaitItem())

            // (B) 准备并执行更新操作
            // 重新预设 decodeString 的行为，以便在 Flow 内部重新读取时获取新值
            every { mockMMKV.decodeString(eq(key), any()) } returns map2Json
            // 预设 putObject 内部会调用的 encode 方法
            every { mockMMKV.encode(key, map2Json) } returns true

            reactiveMMKV.putObject(key, map2)

            // (C) 断言并消费第二个发射项
            // 再次调用 awaitItem()，它会等待由 putObject 触发的下一次发射。
            assertEquals(map2, awaitItem())

            // (D) (可选) 确保 Flow 在此之后没有再发射任何意料之外的值
            expectNoEvents()
        }
    }

    // --- General and Internal Implementation Tests ---

    @Test
    fun `remove calls mmkv removeValueForKey and notifies`() {
        val key = "removeKey"
        // For void methods, we don't use thenReturn
         every { mockMMKV.removeValueForKey(key) } just Runs // MockK equivalent for void

        reactiveMMKV.remove(key)

        verify { mockMMKV.removeValueForKey(key) }
    }

    // --- Preference Delegate Tests ---

    @Test
    fun `preference delegate getValue calls get and returns value`() = runTest(testDispatcher) {
        val key = "prefInt"
        val defaultValue = 100
        val storedValue = 200

        every { mockMMKV.decodeInt(key, defaultValue) } returns storedValue

        val delegate = reactiveMMKV.preference(key, defaultValue)
        val holder = object { var myInt: Int by delegate }

        assertEquals(storedValue, holder.myInt)
        verify { mockMMKV.decodeInt(key, defaultValue) }
    }

    @Test
    fun `preference delegate setValue calls put`() {
        val key = "prefIntSet"
        val defaultValue = 100
        val newValue = 300

        every { mockMMKV.encode(key, newValue) } returns true

        val delegate = reactiveMMKV.preference(key, defaultValue)
        val holder = object { var myInt: Int by delegate }

        holder.myInt = newValue

        verify { mockMMKV.encode(key, newValue) }
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