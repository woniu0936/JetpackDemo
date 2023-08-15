package com.demo.jetpack.mavericks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMavericksDemo01Binding
import com.demo.jetpack.extension.viewBindings

class Mavericks01Activity : AppCompatActivity() {

    private val mBinding by viewBindings(ActivityMavericksDemo01Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        supportFragmentManager.beginTransaction()
            .replace(mBinding.flContainer.id, Mavericks01Fragment(), "Mavericks01Fragment")
            .commit()
    }

}

