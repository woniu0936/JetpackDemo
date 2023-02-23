package com.demo.jetpack.lifecycle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.ViewModelLifecycle

class LifecycleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(LifeComponent())
        
    }

}