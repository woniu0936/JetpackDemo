package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMotionLayout01Binding
import com.demo.jetpack.core.extension.viewBindings

/**
 * 使用layout/motion_01_start和layout/motion_01_end描述动画的开始和结尾
 */
class MotionLayoutActivity01 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout01Binding by viewBindings(ActivityMotionLayout01Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}