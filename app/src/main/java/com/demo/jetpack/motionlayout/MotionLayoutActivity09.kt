package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMotionLayout09Binding
import com.demo.jetpack.core.extension.viewBindings

/**
 * 将MotionLayout和CoordinatorLayout结合使用
 */
class MotionLayoutActivity09 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout09Binding by viewBindings(ActivityMotionLayout09Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}