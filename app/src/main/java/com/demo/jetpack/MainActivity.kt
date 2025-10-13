package com.demo.jetpack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.core.logger.logd
import com.demo.jetpack.core.extension.startActivity
import com.demo.jetpack.core.extension.viewBindings
import com.demo.jetpack.databinding.ActivityMainBinding
import com.demo.jetpack.databinding.ActivityMainBinding.inflate
import com.demo.jetpack.datastore.DataStoreActivity
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
import com.demo.jetpack.navigation.basic.NavigationActivity
import com.demo.jetpack.paging.PagingActivity
import com.demo.jetpack.scroll.ScrollActivity
import com.demo.jetpack.viewpager.ViewPager2Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

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
            startActivity<PagingActivity>(this@MainActivity)
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

        btnNavigation.setOnClickListener {
            startActivity<NavigationActivity>(this@MainActivity)
        }

        btnScroll.setOnClickListener {
            startActivity<ScrollActivity>(this@MainActivity)
        }

        btnDatastore.setOnClickListener {
            startActivity<DataStoreActivity>(this@MainActivity)
        }

        scroll.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//            logD("NMNMNMNXXXIUYIU") { "canScrollVertically(1): ${scroll.canScrollVertically(1)}, canScrollVertically(-1): ${scroll.canScrollVertically(-1)}" }
        }

        scroll.setOnTouchListener { v, event ->
            logd("NMNMNMNXXXIUYIU") { "canScrollVertically(1): ${scroll.canScrollVertically(1)}, canScrollVertically(-1): ${scroll.canScrollVertically(-1)}" }
            false
        }

    }

    fun test() {
        val h1 = H("aaaaaa")
        val h2 = H("bbbbbb")
        val h3 = H("cccccc")
        val h4 = H("dddddd")
        val h5 = H("eeeeee")
    }

    data class H(val a: String = "")
    data class O(val a: Int = 0)

    fun getH() = flow {
        delay(2000)
        emit(H("aaaaaa"))
    }

    suspend fun getO(): O {
        return withContext(Dispatchers.IO) {
            delay(1000)
            O(1)
        }
    }

}