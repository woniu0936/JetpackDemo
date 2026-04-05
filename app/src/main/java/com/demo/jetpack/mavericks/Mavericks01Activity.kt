package com.demo.jetpack.mavericks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.ActivityMavericksDemo01Binding

class Mavericks01Activity : AppCompatActivity() {

    private val mBinding by viewBinding(ActivityMavericksDemo01Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        supportFragmentManager.beginTransaction()
            .replace(mBinding.flContainer.id, Mavericks01Fragment(), "Mavericks01Fragment")
            .commit()
    }

}

