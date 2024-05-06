package com.demo.jetpack.core.extension

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

object GsonKt {
    val gson: Gson = GsonBuilder().setLenient().create()

    inline fun <reified T> fromJson(json: String): T {
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(json, type)
    }

     fun <T> fromJson2(json: String): T {
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(json, type)
    }

    inline fun <reified T> fromJson(jsonElement: JsonElement): T {
        val type = object : TypeToken<T>() {}.type
        return gson.fromJson(jsonElement, type)
    }

    fun toJson(any: Any): String = gson.toJson(this)

    fun toJson(jsonElement: JsonElement): String = gson.toJson(this)
}

inline fun <reified T> fromJson(json: String): T = GsonKt.fromJson(json)

inline fun <reified T> fromJson(jsonElement: JsonElement): T = GsonKt.fromJson(jsonElement)

fun Any.toJson(): String = GsonKt.toJson(this)

fun JsonElement.toJson(): String = GsonKt.toJson(this)

