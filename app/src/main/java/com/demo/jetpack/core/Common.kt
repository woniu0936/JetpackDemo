package com.demo.jetpack.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

inline fun <reified T : AppCompatActivity> AppCompatActivity.startActivity(context: Context, bundle: Bundle? = null) {
    Intent(context, T::class.java).apply {
        bundle?.let {
            putExtras(it)
        }
    }.also {
        startActivity(it)
    }
}