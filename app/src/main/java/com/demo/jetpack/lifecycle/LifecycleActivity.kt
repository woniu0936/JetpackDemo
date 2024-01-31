package com.demo.jetpack.lifecycle

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.extension.logD
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LifecycleActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LifecycleActivity"
    }

    @Inject
    lateinit var mLifeComponent: ActivityLifeComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(mLifeComponent)
        ValueAnimator.ofInt(1, 100).apply {
            repeatCount = ValueAnimator.INFINITE
            bindLifecycle(this@LifecycleActivity)
            duration = 5 * 1000
            addUpdateListener {
                val value = it.animatedValue as Int
                logD(TAG) { "animator value: $value" }
            }
        }.start()
    }

    override fun onDestroy() {
        logD(TAG) { "activity onDestroy" }
        super.onDestroy()
    }

}