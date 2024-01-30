package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMotionLayout08Binding
import com.demo.jetpack.core.extension.viewBindings

/**
 * 将MotionLayout和CoordinatorLayout结合使用
 */
class MotionLayoutActivity08 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout08Binding by viewBindings(ActivityMotionLayout08Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}