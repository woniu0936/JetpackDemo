package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMotionLayout04Binding
import com.demo.jetpack.core.extension.viewBindings

/**
 * 两张图片渐变切换
 */
class MotionLayoutActivity04 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout04Binding by viewBindings(ActivityMotionLayout04Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}