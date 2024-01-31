package com.demo.jetpack.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.extension.viewBindings
import com.demo.jetpack.databinding.ActivityNewBinding

class NewActivity : AppCompatActivity() {

    private val mBinding: ActivityNewBinding by viewBindings(ActivityNewBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
    }

}