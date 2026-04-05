package com.demo.jetpack.navigation.basic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.ActivityNewBinding

class NewActivity : AppCompatActivity() {

    private val mBinding: ActivityNewBinding by viewBinding(ActivityNewBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
    }

}