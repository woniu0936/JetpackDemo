package com.demo.jetpack.navigation.basic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.ActivityNavigationBinding

class NavigationActivity : AppCompatActivity() {

    private val mBinding: ActivityNavigationBinding by viewBinding(ActivityNavigationBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
    }

}