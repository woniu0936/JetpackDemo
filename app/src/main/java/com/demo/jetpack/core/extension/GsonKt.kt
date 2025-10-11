package com.demo.jetpack.core.extension

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * 共享的 Gson 实例。
 * 注意：这里使用了 setLenient()，它会尝试解析格式不规范的 JSON。
 * 在生产环境中，如果需要严格的 JSON 格式校验，请考虑移除 .setLenient()。
 */
@PublishedApi
internal val gson: Gson = GsonBuilder().create()

/**
 * 共享的、用于调试打印的 Gson 实例 (输出格式化的 JSON)。
 */
private val prettyGson: Gson = GsonBuilder()
    .setLenient()
    .setPrettyPrinting() // <--- 关键配置：启用优美的打印格式
    .create()

// =======================================================================================
// ===                         1. 核心序列化/反序列化扩展                          ===
// =======================================================================================

fun Any.toJson(): String = gson.toJson(this)
fun JsonElement.toJson(): String = gson.toJson(this)

inline fun <reified T> String.fromJson(): T {
    val type: Type = object : TypeToken<T>() {}.type
    return gson.fromJson(this, type)
}

inline fun <reified T> JsonElement.fromJson(): T {
    val type: Type = object : TypeToken<T>() {}.type
    return gson.fromJson(this, type)
}

inline fun <reified T> String.fromJsonOrNull(): T? {
    return try { fromJson<T>() } catch (e: Exception) { null }
}

inline fun <reified T> JsonElement.fromJsonOrNull(): T? {
    return try { fromJson<T>() } catch (e: Exception) { null }
}

// =======================================================================================
// ===                       2. 从 JSON 字符串中提取指定字段                          ===
// =======================================================================================

fun String.getJsonObject(key: String): JsonObject? {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getJsonObject(key) // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        null
    }
}

fun String.getJsonArray(key: String): JsonArray? {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getJsonArray(key) // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        null
    }
}

fun String.getString(key: String, defaultValue: String = ""): String {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getString(key, defaultValue) ?: defaultValue // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        defaultValue
    }
}

fun String.getInt(key: String, defaultValue: Int = 0): Int {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getInt(key, defaultValue) ?: defaultValue // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        defaultValue
    }
}

fun String.getLong(key: String, defaultValue: Long = 0L): Long {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getLong(key, defaultValue) ?: defaultValue // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        defaultValue
    }
}

fun String.getBoolean(key: String, defaultValue: Boolean = false): Boolean {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getBoolean(key, defaultValue) ?: defaultValue // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        defaultValue
    }
}

fun String.getDouble(key: String, defaultValue: Double = 0.0): Double {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getDouble(key, defaultValue) ?: defaultValue // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        defaultValue
    }
}

fun String.getStringOrNull(key: String): String? {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getStringOrNull(key) // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        null
    }
}

fun String.getIntOrNull(key: String): Int? {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getIntOrNull(key) // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        null
    }
}

fun String.getLongOrNull(key: String): Long? {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getLongOrNull(key) // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        null
    }
}

fun String.getBooleanOrNull(key: String): Boolean? {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getBooleanOrNull(key) // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        null
    }
}

fun String.getDoubleOrNull(key: String): Double? {
    return try {
        JsonParser.parseString(this)
            .takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getDoubleOrNull(key) // 复用 JsonObject 的扩展
    } catch (e: Exception) {
        null
    }
}

// =======================================================================================
// ===                       3. 从 JsonObject 中提取指定字段                           ===
// =======================================================================================

fun JsonObject.getJsonObject(key: String): JsonObject? {
    return this.get(key)
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
}

fun JsonObject.getJsonArray(key: String): JsonArray? {
    return this.get(key)
        ?.takeIf { it.isJsonArray }
        ?.asJsonArray
}

fun JsonObject.getString(key: String, defaultValue: String = ""): String {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString
        ?: defaultValue
}

fun JsonObject.getInt(key: String, defaultValue: Int = 0): Int {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asInt
        ?: defaultValue
}

fun JsonObject.getLong(key: String, defaultValue: Long = 0L): Long {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asLong
        ?: defaultValue
}

fun JsonObject.getBoolean(key: String, defaultValue: Boolean = false): Boolean {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
        ?.asBoolean
        ?: defaultValue
}

fun JsonObject.getDouble(key: String, defaultValue: Double = 0.0): Double {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asDouble
        ?: defaultValue
}

fun JsonObject.getStringOrNull(key: String): String? {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString
}

fun JsonObject.getIntOrNull(key: String): Int? {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asInt
}

fun JsonObject.getLongOrNull(key: String): Long? {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asLong
}

fun JsonObject.getBooleanOrNull(key: String): Boolean? {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
        ?.asBoolean
}

fun JsonObject.getDoubleOrNull(key: String): Double? {
    return this.get(key)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asDouble
}


// =======================================================================================
// ===                       4. 从 JsonArray 中提取指定索引的元素                       ===
// =======================================================================================

fun JsonArray.getJsonObject(index: Int): JsonObject? {
    return this.get(index)
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
}

fun JsonArray.getJsonArray(index: Int): JsonArray? {
    return this.get(index)
        ?.takeIf { it.isJsonArray }
        ?.asJsonArray
}

fun JsonArray.getString(index: Int, defaultValue: String = ""): String {
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString
        ?: defaultValue
}

fun JsonArray.getInt(index: Int, defaultValue: Int = 0): Int {
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asInt
        ?: defaultValue
}

fun JsonArray.getLong(index: Int, defaultValue: Long = 0L): Long {
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asLong
        ?: defaultValue
}

fun JsonArray.getBoolean(index: Int, defaultValue: Boolean = false): Boolean {
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
        ?.asBoolean
        ?: defaultValue
}

fun JsonArray.getDouble(index: Int, defaultValue: Double = 0.0): Double {
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asDouble
        ?: defaultValue
}

fun JsonArray.getStringOrNull(index: Int): String? {
    if (index < 0 || index >= this.size()) {
        return null
    }
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString
}

fun JsonArray.getIntOrNull(index: Int): Int? {
    if (index < 0 || index >= this.size()) {
        return null
    }
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asInt
}

fun JsonArray.getLongOrNull(index: Int): Long? {
    if (index < 0 || index >= this.size()) {
        return null
    }
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asLong
}

fun JsonArray.getBooleanOrNull(index: Int): Boolean? {
    if (index < 0 || index >= this.size()) {
        return null
    }
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
        ?.asBoolean
}

fun JsonArray.getDoubleOrNull(index: Int): Double? {
    if (index < 0 || index >= this.size()) {
        return null
    }
    return this.get(index)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asDouble
}

// =======================================================================================
// ===                       5. JSON 格式化 (用于调试打印)                          ===
// =======================================================================================

/**
 * 将任何对象序列化为格式优美的 JSON 字符串，方便日志打印和调试。
 *
 * 用法: `println(myObject.toJsonFormatted())`
 */
fun Any.toJsonFormatted(): String = prettyGson.toJson(this)

/**
 * 将 JsonElement 序列化为格式优美的 JSON 字符串。
 *
 * 用法: `Log.d("API_RESPONSE", responseJson.toJsonFormatted())`
 */
fun JsonElement.toJsonFormatted(): String = prettyGson.toJson(this)

/**
 * 将一个可能未经格式化的 JSON 字符串转换为格式优美的版本。
 * 如果输入的字符串不是有效的 JSON，将安全地返回原始字符串。
 *
 * 用法: `val formatted = messyJsonString.toJsonFormatted()`
 */
fun String.toJsonFormatted(): String {
    return try {
        val jsonElement = JsonParser.parseString(this)
        prettyGson.toJson(jsonElement)
    } catch (e: Exception) {
        // 如果解析失败，返回原始字符串，而不是抛出异常
        this
    }
}
