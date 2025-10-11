package com.demo.jetpack.core.extension

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GsonKtTest {

    // Test data
    private val jsonString = """
        {
            "name": "Test Name",
            "age": 30,
            "isStudent": true,
            "height": 175.5,
            "score": 1234567890123,
            "address": {
                "street": "Test Street",
                "zip": "12345"
            },
            "hobbies": ["reading", "hiking"],
            "nullField": null,
            "emptyString": ""
        }
    """.trimIndent()

    private val jsonObject = JsonParser.parseString(jsonString).asJsonObject
    private val jsonArray = JsonParser.parseString("""["item1", 123, true, 45.6, null]""").asJsonArray

    // --- String extensions ---

    @Test
    fun `getStringOrNull from String should return value when key exists and is string`() {
        // 场景：从 JSON 字符串中获取一个存在的、类型为字符串的字段。
        // 预期：应成功返回该字符串值。
        assertEquals("Test Name", jsonString.getStringOrNull("name"))
    }

    @Test
    fun `getStringOrNull from String should return null when key exists but is not string`() {
        // 场景：从 JSON 字符串中获取一个存在的、但类型不是字符串的字段（例如数字）。
        // 预期：应返回 null。
        assertNull(jsonString.getStringOrNull("age"))
    }

    @Test
    fun `getStringOrNull from String should return null when key does not exist`() {
        // 场景：从 JSON 字符串中获取一个不存在的字段。
        // 预期：应返回 null。
        assertNull(jsonString.getStringOrNull("nonExistent"))
    }

    @Test
    fun `getStringOrNull from String should return null when key is null`() {
        // 场景：从 JSON 字符串中获取一个存在的、但值为 null 的字段。
        // 预期：应返回 null。
        assertNull(jsonString.getStringOrNull("nullField"))
    }

    @Test
    fun `getIntOrNull from String should return value when key exists and is int`() {
        // 场景：从 JSON 字符串中获取一个存在的、类型为整数的字段。
        // 预期：应成功返回该整数值。
        assertEquals(30, jsonString.getIntOrNull("age"))
    }

    @Test
    fun `getIntOrNull from String should return null when key exists but is not int`() {
        // 场景：从 JSON 字符串中获取一个存在的、但类型不是整数的字段（例如字符串）。
        // 预期：应返回 null。
        assertNull(jsonString.getIntOrNull("name"))
    }

    @Test
    fun `getIntOrNull from String should return null when key does not exist`() {
        // 场景：从 JSON 字符串中获取一个不存在的字段。
        // 预期：应返回 null。
        assertNull(jsonString.getIntOrNull("nonExistent"))
    }

    @Test
    fun `getLongOrNull from String should return value when key exists and is long`() {
        // 场景：从 JSON 字符串中获取一个存在的、类型为长整数的字段。
        // 预期：应成功返回该长整数值。
        assertEquals(1234567890123L, jsonString.getLongOrNull("score"))
    }

    @Test
    fun `getLongOrNull from String should return null when key exists but is not long`() {
        // 场景：从 JSON 字符串中获取一个存在的、但类型不是长整数的字段（例如字符串）。
        // 预期：应返回 null。
        assertNull(jsonString.getLongOrNull("name"))
    }

    @Test
    fun `getBooleanOrNull from String should return value when key exists and is boolean`() {
        // 场景：从 JSON 字符串中获取一个存在的、类型为布尔值的字段。
        // 预期：应成功返回该布尔值。
        assertEquals(true, jsonString.getBooleanOrNull("isStudent"))
    }

    @Test
    fun `getBooleanOrNull from String should return null when key exists but is not boolean`() {
        // 场景：从 JSON 字符串中获取一个存在的、但类型不是布尔值的字段（例如字符串）。
        // 预期：应返回 null。
        assertNull(jsonString.getBooleanOrNull("name"))
    }

    @Test
    fun `getDoubleOrNull from String should return value when key exists and is double`() {
        // 场景：从 JSON 字符串中获取一个存在的、类型为双精度浮点数的字段。
        // 预期：应成功返回该双精度浮点数值。
        val result = jsonString.getDoubleOrNull("height")
        org.junit.Assert.assertTrue("Expected a non-null Double", result != null)
        assertEquals(175.5, result!!, 0.0)
    }

    @Test
    fun `getDoubleOrNull from String should return null when key exists but is not double`() {
        // 场景：从 JSON 字符串中获取一个存在的、但类型不是双精度浮点数的字段（例如字符串）。
        // 预期：应返回 null。
        assertNull(jsonString.getDoubleOrNull("name"))
    }

    // --- JsonObject extensions ---

    @Test
    fun `getStringOrNull from JsonObject should return value when key exists and is string`() {
        // 场景：从 JsonObject 中获取一个存在的、类型为字符串的字段。
        // 预期：应成功返回该字符串值。
        assertEquals("Test Name", jsonObject.getStringOrNull("name"))
    }

    @Test
    fun `getStringOrNull from JsonObject should return null when key exists but is not string`() {
        // 场景：从 JsonObject 中获取一个存在的、但类型不是字符串的字段（例如数字）。
        // 预期：应返回 null。
        assertNull(jsonObject.getStringOrNull("age"))
    }

    @Test
    fun `getStringOrNull from JsonObject should return null when key does not exist`() {
        // 场景：从 JsonObject 中获取一个不存在的字段。
        // 预期：应返回 null。
        assertNull(jsonObject.getStringOrNull("nonExistent"))
    }

    @Test
    fun `getStringOrNull from JsonObject should return null when key is null`() {
        // 场景：从 JsonObject 中获取一个存在的、但值为 null 的字段。
        // 预期：应返回 null。
        assertNull(jsonObject.getStringOrNull("nullField"))
    }

    @Test
    fun `getIntOrNull from JsonObject should return value when key exists and is int`() {
        // 场景：从 JsonObject 中获取一个存在的、类型为整数的字段。
        // 预期：应成功返回该整数值。
        assertEquals(30, jsonObject.getIntOrNull("age"))
    }

    @Test
    fun `getIntOrNull from JsonObject should return null when key exists but is not int`() {
        // 场景：从 JsonObject 中获取一个存在的、但类型不是整数的字段（例如字符串）。
        // 预期：应返回 null。
        assertNull(jsonObject.getIntOrNull("name"))
    }

    @Test
    fun `getLongOrNull from JsonObject should return value when key exists and is long`() {
        // 场景：从 JsonObject 中获取一个存在的、类型为长整数的字段。
        // 预期：应成功返回该长整数值。
        assertEquals(1234567890123L, jsonObject.getLongOrNull("score"))
    }

    @Test
    fun `getBooleanOrNull from JsonObject should return value when key exists and is boolean`() {
        // 场景：从 JsonObject 中获取一个存在的、类型为布尔值的字段。
        // 预期：应成功返回该布尔值。
        assertEquals(true, jsonObject.getBooleanOrNull("isStudent"))
    }

    @Test
    fun `getDoubleOrNull from JsonObject should return value when key exists and is double`() {
        // 场景：从 JsonObject 中获取一个存在的、类型为双精度浮点数的字段。
        // 预期：应成功返回该双精度浮点数值。
        val result = jsonObject.getDoubleOrNull("height")
        org.junit.Assert.assertTrue("Expected a non-null Double", result != null)
        assertEquals(175.5, result!!, 0.0)
    }

    // --- JsonArray extensions ---

    @Test
    fun `getStringOrNull from JsonArray should return value when index exists and is string`() {
        // 场景：从 JsonArray 中获取一个存在的、类型为字符串的元素。
        // 预期：应成功返回该字符串值。
        assertEquals("item1", jsonArray.getStringOrNull(0))
    }

    @Test
    fun `getStringOrNull from JsonArray should return null when index exists but is not string`() {
        // 场景：从 JsonArray 中获取一个存在的、但类型不是字符串的元素（例如数字）。
        // 预期：应返回 null。
        assertNull(jsonArray.getStringOrNull(1)) // 123 is an Int
    }

    @Test
    fun `getStringOrNull from JsonArray should return null when index is out of bounds`() {
        // 场景：从 JsonArray 中获取一个索引越界的元素。
        // 预期：应返回 null。
        assertNull(jsonArray.getStringOrNull(10))
    }

    @Test
    fun `getStringOrNull from JsonArray should return null when element is null`() {
        // 场景：从 JsonArray 中获取一个存在的、但值为 null 的元素。
        // 预期：应返回 null。
        assertNull(jsonArray.getStringOrNull(4)) // null
    }

    @Test
    fun `getIntOrNull from JsonArray should return value when index exists and is int`() {
        // 场景：从 JsonArray 中获取一个存在的、类型为整数的元素。
        // 预期：应成功返回该整数值。
        assertEquals(123, jsonArray.getIntOrNull(1))
    }

    @Test
    fun `getIntOrNull from JsonArray should return null when index exists but is not int`() {
        // 场景：从 JsonArray 中获取一个存在的、但类型不是整数的元素（例如字符串）。
        // 预期：应返回 null。
        assertNull(jsonArray.getIntOrNull(0)) // "item1" is a String
    }

    @Test
    fun `getLongOrNull from JsonArray should return value when index exists and is long`() {
        // 场景：从 JsonArray 中获取一个存在的、类型为长整数的元素。
        // 预期：应成功返回该长整数值。
        assertEquals(123L, jsonArray.getLongOrNull(1))
    }

    @Test
    fun `getBooleanOrNull from JsonArray should return value when index exists and is boolean`() {
        // 场景：从 JsonArray 中获取一个存在的、类型为布尔值的元素。
        // 预期：应成功返回该布尔值。
        assertEquals(true, jsonArray.getBooleanOrNull(2))
    }

    @Test
    fun `getDoubleOrNull from JsonArray should return value when index exists and is double`() {
        // 场景：从 JsonArray 中获取一个存在的、类型为双精度浮点数的元素。
        // 预期：应成功返回该双精度浮点数值。
        val result = jsonArray.getDoubleOrNull(3)
        org.junit.Assert.assertTrue("Expected a non-null Double", result != null)
        assertEquals(45.6, result!!, 0.0)
    }

    @Test
    fun `getIntOrNull from JsonArray should return null when element is null`() {
        // 场景：从 JsonArray 中获取一个存在的、但值为 null 的元素，期望得到 Int? 类型。
        // 预期：应返回 null。
        assertNull(jsonArray.getIntOrNull(4)) // null
    }

    @Test
    fun `getLongOrNull from JsonArray should return null when element is null`() {
        // 场景：从 JsonArray 中获取一个存在的、但值为 null 的元素，期望得到 Long? 类型。
        // 预期：应返回 null。
        assertNull(jsonArray.getLongOrNull(4)) // null
    }

    @Test
    fun `getBooleanOrNull from JsonArray should return null when element is null`() {
        // 场景：从 JsonArray 中获取一个存在的、但值为 null 的元素，期望得到 Boolean? 类型。
        // 预期：应返回 null。
        assertNull(jsonArray.getBooleanOrNull(4)) // null
    }

    @Test
    fun `getDoubleOrNull from JsonArray should return null when element is null`() {
        // 场景：从 JsonArray 中获取一个存在的、但值为 null 的元素，期望得到 Double? 类型。
        // 预期：应返回 null。
        assertNull(jsonArray.getDoubleOrNull(4)) // null
    }
}