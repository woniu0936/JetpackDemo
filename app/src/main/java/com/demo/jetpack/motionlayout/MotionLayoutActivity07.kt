package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMotionLayout07Binding
import com.demo.jetpack.core.extension.viewBindings

/**
 * 关键帧控制路径变成曲线，并且添加缩放和旋转
 */
class MotionLayoutActivity07 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout07Binding by viewBindings(ActivityMotionLayout07Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}