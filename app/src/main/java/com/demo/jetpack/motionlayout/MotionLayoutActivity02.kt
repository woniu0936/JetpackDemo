package com.demo.jetpack.motionlayout

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMotionLayout02Binding
import com.demo.jetpack.core.extension.viewBindings

/**
 * 直接在@xml/motion_scene_02中描述动画已经动画开始和结束的样子
 */
class MotionLayoutActivity02 : AppCompatActivity() {

    private val mBinding: ActivityMotionLayout02Binding by viewBindings(ActivityMotionLayout02Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

    }

}