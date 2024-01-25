package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMotionLayout05Binding
import com.demo.jetpack.extension.viewBindings

/**
 * 控制饱和度变化
 */
class MotionLayoutActivity05 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout05Binding by viewBindings(ActivityMotionLayout05Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}