package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.ActivityMotionLayout06Binding

/**
 * 关键帧控制路径变成曲线
 */
class MotionLayoutActivity06 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout06Binding by viewBinding(ActivityMotionLayout06Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}