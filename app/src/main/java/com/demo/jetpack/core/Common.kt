package com.demo.jetpack.core

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