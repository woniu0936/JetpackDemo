package com.demo.jetpack

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.demo.jetpack.databinding.ActivityMainBinding
import com.demo.jetpack.hilt.HiltActivity
import com.demo.jetpack.lifecycle.LifecycleActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initEvent()
    }

    private fun initEvent() = with(mBinding) {
        btnHilt.setOnClickListener {
            var intent = Intent(this@MainActivity, HiltActivity::class.java)
            startActivity(intent)
        }

        btnLifecycle.setOnClickListener {
            var intent = Intent(this@MainActivity, LifecycleActivity::class.java)
            startActivity(intent)
        }
    }

}