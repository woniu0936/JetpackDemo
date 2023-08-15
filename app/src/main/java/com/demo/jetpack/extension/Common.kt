package com.demo.jetpack.extension

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

inline fun <reified T : AppCompatActivity> AppCompatActivity.startActivity(context: Context, block: Bundle.() -> Unit = {}) {
    val bundle = Bundle().apply { block() }
    Intent(context, T::class.java).apply {
        putExtras(bundle)
    }.also {
        startActivity(it)
    }
}

val Any?.isNotNull
    get() = this != null

val Any?.isNull
    get() = this == null

inline fun <reified T> T?.ifNull(block: () -> Unit) {
    if (this.isNull) {
        block.invoke()
    }
}

inline fun <reified T> T?.ifNotNull(block: T.() -> Unit) {
    if (this.isNotNull) {
        block.invoke(this!!)
    }
}