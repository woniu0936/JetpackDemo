package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMotionLayout03Binding
import com.demo.jetpack.extension.viewBindings

/**
 * 直接在@xml/motion_scene_02中描述动画已经动画开始和结束的样子
 */
class MotionLayoutActivity03 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout03Binding by viewBindings(ActivityMotionLayout03Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}