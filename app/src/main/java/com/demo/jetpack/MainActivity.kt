package com.demo.jetpack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.startActivity
import com.demo.jetpack.core.viewBindings
import com.demo.jetpack.databinding.ActivityMainBinding
import com.demo.jetpack.databinding.ActivityMainBinding.inflate
import com.demo.jetpack.hilt.HiltActivity
import com.demo.jetpack.lifecycle.LifecycleActivity
import com.demo.jetpack.viewmodel.DemoActivity

class MainActivity : AppCompatActivity() {

    private val mBinding: ActivityMainBinding by viewBindings(::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        initEvent()
    }

    private fun initEvent() = with(mBinding) {
        btnHilt.setOnClickListener {
            startActivity<HiltActivity>(this@MainActivity)
        }

        btnLifecycle.setOnClickListener {
            startActivity<LifecycleActivity>(this@MainActivity)
        }


        btnPaging.setOnClickListener {
            startActivity<DemoActivity>(this@MainActivity)
        }
    }

}