package com.demo.core.common.extension

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Parcelable

fun Activity.optString(key: String, defaultValue: String = "") = lazy {
    this.intent?.getStringExtra(key) ?: defaultValue
}

fun Activity.optInt(key: String, defaultValue: Int = 0) = lazy {
    this.intent?.getIntExtra(key, defaultValue) ?: defaultValue
}

fun Activity.optLong(key: String, defaultValue: Long = 0L) = lazy {
    this.intent?.getLongExtra(key, defaultValue) ?: defaultValue
}

fun Activity.optBoolean(key: String, defaultValue: Boolean = false) = lazy {
    this.intent?.getBooleanExtra(key, defaultValue) ?: defaultValue
}

fun Intent?.optBoolean(key: String, defaultValue: Boolean = false): Boolean {
    return this?.getBooleanExtra(key, defaultValue) ?: defaultValue
}

fun Intent?.optInt(key: String, defaultValue: Int = 0): Int {
    return this?.getIntExtra(key, defaultValue) ?: defaultValue
}

fun Intent?.optLong(key: String, defaultValue: Long = 0): Long {
    return this?.getLongExtra(key, defaultValue) ?: defaultValue
}

fun Intent?.optString(key: String, defaultValue: String = ""): String {
    return this?.getStringExtra(key) ?: defaultValue
}

//fun <T> Intent?.optParcelable(key: String, defaultValue: T): T {
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        this?.getParcelableExtra(key, defaultValue!!::class.java) ?: defaultValue
//    } else {
//        this?.getParcelableExtra(key) ?: defaultValue
//    }
//}
//
//inline fun <reified T : Parcelable> Intent?.optParcelable(key: String): T? {
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        this?.getParcelableExtra(key, T::class.java)
//    } else {
//        this?.getParcelableExtra(key)
//    }
//}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent?.optParcelableArrayList(
    key: String,
    defaultValue: ArrayList<T> = ArrayList(),
): ArrayList<T> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    this?.getParcelableArrayListExtra(key, T::class.java) ?: defaultValue
} else {
    this?.getParcelableArrayListExtra(key) ?: defaultValue
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Activity?.optParcelableArrayList(
    key: String,
    defaultValue: ArrayList<T> = ArrayList(),
) = lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this?.intent?.getParcelableArrayListExtra(key, T::class.java) ?: defaultValue
    } else {
        this?.intent?.getParcelableArrayListExtra(key) ?: defaultValue
    }
}

//@Suppress("DEPRECATION")
//fun Intent?.optStringArrayListExtra(
//    key: String,
//    defaultValue: ArrayList<String> = ArrayList(),
//): ArrayList<String> {
//    return this?.getStringArrayListExtra(key) ?: defaultValue
//}
