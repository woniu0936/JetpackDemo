package com.demo.jetpack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import com.demo.core.logger.logger // Add this import

class MainActivity : AppCompatActivity() {

    private val mBinding: ActivityMainBinding by viewBindings(::inflate)
    private val log by lazy { logger() } // Add logger instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        initEvent()
        log.d { "MainActivity onCreate called." } // Example log
    }

    private fun initEvent() = with(mBinding) {
        btnHilt.setOnClickListener {
            log.d { "Hilt button clicked." }
            startActivity<HiltActivity>(this@MainActivity)
        }

        btnLifecycle.setOnClickListener {
            log.d { "Lifecycle button clicked." }
            startActivity<LifecycleActivity>(this@MainActivity)
        }

        btnPaging.setOnClickListener {
            log.d { "Paging button clicked." }
            startActivity<PagingActivity>(this@MainActivity)
        }

        btnMavericks.setOnClickListener {
            log.d { "Mavericks button clicked." }
            startActivity<Mavericks01Activity>(this@MainActivity)
        }

        btnMotionLayout01.setOnClickListener {
            log.d { "MotionLayout01 button clicked." }
            startActivity<MotionLayoutActivity01>(this@MainActivity)
        }

        btnMotionLayout02.setOnClickListener {
            log.d { "MotionLayout02 button clicked." }
            startActivity<MotionLayoutActivity02>(this@MainActivity)
        }

        btnMotionLayout03.setOnClickListener {
            log.d { "MotionLayout03 button clicked." }
            startActivity<MotionLayoutActivity03>(this@MainActivity)
        }

        btnMotionLayout04.setOnClickListener {
            log.d { "MotionLayout04 button clicked." }
            startActivity<MotionLayoutActivity04>(this@MainActivity)
        }

        btnMotionLayout05.setOnClickListener {
            log.d { "MotionLayout05 button clicked." }
            startActivity<MotionLayoutActivity05>(this@MainActivity)
        }

        btnMotionLayout06.setOnClickListener {
            log.d { "MotionLayout06 button clicked." }
            startActivity<MotionLayoutActivity06>(this@MainActivity)
        }

        btnMotionLayout07.setOnClickListener {
            log.d { "MotionLayout07 button clicked." }
            startActivity<MotionLayoutActivity07>(this@MainActivity)
        }

        btnMotionLayout08.setOnClickListener {
            log.d { "MotionLayout08 button clicked." }
            startActivity<MotionLayoutActivity08>(this@MainActivity)
        }

        btnMotionLayout09.setOnClickListener {
            log.d { "MotionLayout09 button clicked." }
            startActivity<MotionLayoutActivity09>(this@MainActivity)
        }

        btnViewPager.setOnClickListener {
            log.d { "ViewPager button clicked." }
            startActivity<ViewPager2Activity>(this@MainActivity)
        }

        btnNavigation.setOnClickListener {
            log.d { "Navigation button clicked." }
            startActivity<NavigationActivity>(this@MainActivity)
        }

        btnScroll.setOnClickListener {
            log.d { "Scroll button clicked." }
            startActivity<ScrollActivity>(this@MainActivity)
        }

        btnDatastore.setOnClickListener {
            log.d { "Datastore button clicked." }
            startActivity<DataStoreActivity>(this@MainActivity)
        }

        scroll.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            log.d { "canScrollVertically(1): ${scroll.canScrollVertically(1)}, canScrollVertically(-1): ${scroll.canScrollVertically(-1)}" }
        }

        scroll.setOnTouchListener { v, event ->
            log.d { "canScrollVertically(1): ${scroll.canScrollVertically(1)}, canScrollVertically(-1): ${scroll.canScrollVertically(-1)}" }
            false
        }

    }

    fun test() {
        val h1 = H("aaaaaa")
        val h2 = H("bbbbbb")
        val h3 = H("cccccc")
        val h4 = H("dddddd")
        val h5 = H("eeeeee")
        log.d { "Test function called with H instances." }
    }

    data class H(val a: String = "")
    data class O(val a: Int = 0)

    fun getH() = flow {
        delay(2000)
        emit(H("aaaaaa"))
        log.d { "getH flow emitted a value." }
    }

    suspend fun getO(): O {
        return withContext(Dispatchers.IO) {
            delay(1000)
            log.d { "getO suspend function returning O(1)." }
            O(1)
        }
    }

}