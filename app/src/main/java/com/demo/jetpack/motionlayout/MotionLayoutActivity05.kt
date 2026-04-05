package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.ActivityMotionLayout05Binding

/**
 * 控制饱和度变化
 */
class MotionLayoutActivity05 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout05Binding by viewBinding(ActivityMotionLayout05Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}