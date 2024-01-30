package com.demo.jetpack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.databinding.ActivityMainBinding
import com.demo.jetpack.databinding.ActivityMainBinding.inflate
import com.demo.jetpack.core.extension.startActivity
import com.demo.jetpack.core.extension.viewBindings
import com.demo.jetpack.hilt.HiltActivity
import com.demo.jetpack.lifecycle.LifecycleActivity
import com.demo.jetpack.mavericks.Mavericks01Activity
import com.demo.jetpack.motionlayout.MotionLayoutActivity01
import com.demo.jetpack.motionlayout.MotionLayoutActivity02
import com.demo.jetpack.motionlayout.MotionLayoutActivity03
import com.demo.jetpack.motionlayout.MotionLayoutActivity04
import com.demo.jetpack.motionlayout.MotionLayoutActivity05
import com.demo.jetpack.motionlayout.MotionLayoutActivity06
import com.demo.jetpack.motionlayout.MotionLayoutActivity07
import com.demo.jetpack.motionlayout.MotionLayoutActivity08
import com.demo.jetpack.motionlayout.MotionLayoutActivity09
import com.demo.jetpack.viewmodel.DemoActivity
import com.demo.jetpack.viewpager.ViewPager2Activity

class MainActivity : AppCompatActivity() {

    private val mBinding: ActivityMainBinding by viewBindings(::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        initEvent()
    }

    private fun initEvent() = with(mBinding) {
        btnHilt.setOnClickListener {
            startActivity<HiltActivity>(this@MainActivity)
        }

        btnLifecycle.setOnClickListener {
            startActivity<LifecycleActivity>(this@MainActivity)
        }

        btnPaging.setOnClickListener {
            startActivity<DemoActivity>(this@MainActivity)
        }

        btnMavericks.setOnClickListener {
            startActivity<Mavericks01Activity>(this@MainActivity)
        }

        btnMotionLayout01.setOnClickListener {
            startActivity<MotionLayoutActivity01>(this@MainActivity)
        }

        btnMotionLayout02.setOnClickListener {
            startActivity<MotionLayoutActivity02>(this@MainActivity)
        }

        btnMotionLayout03.setOnClickListener {
            startActivity<MotionLayoutActivity03>(this@MainActivity)
        }

        btnMotionLayout04.setOnClickListener {
            startActivity<MotionLayoutActivity04>(this@MainActivity)
        }

        btnMotionLayout05.setOnClickListener {
            startActivity<MotionLayoutActivity05>(this@MainActivity)
        }

        btnMotionLayout06.setOnClickListener {
            startActivity<MotionLayoutActivity06>(this@MainActivity)
        }

        btnMotionLayout07.setOnClickListener {
            startActivity<MotionLayoutActivity07>(this@MainActivity)
        }

        btnMotionLayout08.setOnClickListener {
            startActivity<MotionLayoutActivity08>(this@MainActivity)
        }

        btnMotionLayout09.setOnClickListener {
            startActivity<MotionLayoutActivity09>(this@MainActivity)
        }

        btnViewPager.setOnClickListener {
            startActivity<ViewPager2Activity>(this@MainActivity)
        }
    }

}