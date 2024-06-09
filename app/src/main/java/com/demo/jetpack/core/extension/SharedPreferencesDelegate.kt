package com.demo.jetpack.core.extension

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SharedPreferencesDelegate<T>(
    private val context: Context,
    private val spName: String,
    private val defaultValue: T,
    private val key: String? = null,
) : ReadWriteProperty<Any?, T> {

    private val sp: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
        context.getSharedPreferences(spName, Context.MODE_PRIVATE)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val finalKey = key ?: property.name
        return when (defaultValue) {
            is Int -> sp.getInt(finalKey, defaultValue)
            is Long -> sp.getLong(finalKey, defaultValue)
            is Float -> sp.getFloat(finalKey, defaultValue)
            is Boolean -> sp.getBoolean(finalKey, defaultValue)
            is String -> sp.getString(finalKey, defaultValue)
            is Set<*> -> sp.getStringSet(finalKey, defaultValue as? Set<String>)
            else -> throw IllegalStateException("Unsupported type")
        } as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val finalKey = key ?: property.name
        with(sp.edit()) {
            when (value) {
                is Int -> putInt(finalKey, value)
                is Long -> putLong(finalKey, value)
                is Float -> putFloat(finalKey, value)
                is Boolean -> putBoolean(finalKey, value)
                is String -> putString(finalKey, value)
                is Set<*> -> putStringSet(finalKey, value.map { it.toString() }.toHashSet())
                else -> throw IllegalStateException("Unsupported type")
            }
            apply()
        }
    }
}
