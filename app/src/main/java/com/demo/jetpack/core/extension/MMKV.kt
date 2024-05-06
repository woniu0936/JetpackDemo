package com.demo.jetpack.core.extension

import android.content.Context
import com.tencent.mmkv.MMKV
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MMKVDelegate<T>(val key: String, val defaultValue: T) : ReadWriteProperty<Any, T> {

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        MMKV.defaultMMKV().run {
            when (value) {
                is Int -> encode(key, value)
                is Long -> encode(key, value)
                is Float -> encode(key, value)
                is Double -> encode(key, value)
                is Boolean -> encode(key, value)
                is String -> encode(key, value)
                is Set<*> -> encode(key, value.map { it.toString() }.toHashSet())
                else -> value?.toJson()
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T = with(MMKV.defaultMMKV()) {
        val value = when (defaultValue) {
            is Int -> decodeInt(key, defaultValue)
            is Long -> decodeLong(key, defaultValue)
            is Float -> decodeFloat(key, defaultValue)
            is Double -> decodeDouble(key, defaultValue)
            is Boolean -> decodeBool(key, defaultValue)
            is String -> decodeString(key, defaultValue)
            is Set<*> -> decodeStringSet(key, defaultValue as Set<String>)
            else -> {
                decodeString(key)?.let { value ->
                    GsonKt.fromJson2<T>(value)
                } ?: defaultValue
            }
        }
        return@with value as T
    }

}

fun initMMKV(context: Context) {
    MMKV.initialize(context.applicationContext)
}

inline fun <reified T : Any> MMKV.encode(key: String, value: T) {
    when (value) {
        is Int -> encode(key, value)
        is Long -> encode(key, value)
        is Float -> encode(key, value)
        is Double -> encode(key, value)
        is Boolean -> encode(key, value)
        is String -> encode(key, value)
        is Set<*> -> encode(key, value.map { it.toString() }.toHashSet())
        else -> value.toJson()
    }
}

@Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
inline fun <reified T : Any> MMKV.decode(key: String, defaultValue: T): T {
    val value = when (defaultValue) {
        is Int -> decodeInt(key, defaultValue)
        is Long -> decodeLong(key, defaultValue)
        is Float -> decodeFloat(key, defaultValue)
        is Double -> decodeDouble(key, defaultValue)
        is Boolean -> decodeBool(key, defaultValue)
        is String -> decodeString(key, defaultValue)
        is Set<*> -> {
            try {
                decodeStringSet(key, defaultValue as Set<String>)
            } catch (e: Exception) {
                defaultValue
            }
        }

        else -> {
            decodeString(key)?.let { value ->
                fromJson<T>(value)
            } ?: defaultValue
        }
    }
    return value as T
}


