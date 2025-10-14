package com.demo.jetpack.scroll

import android.graphics.Insets
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.demo.core.logger.logD
import com.demo.jetpack.core.extension.viewBindings
import com.demo.jetpack.databinding.ActivityScrollBinding


var canScroll = true

class ScrollActivity : AppCompatActivity() {

    private val mBinding: ActivityScrollBinding by viewBindings(ActivityScrollBinding::inflate)
    private val mFragments by lazy { listOf(IndexFragment.newInstance(0), IndexFragment.newInstance(2), IndexFragment.newInstance(2)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        mBinding.vp2.adapter = Adapter(this, mFragments)
        canScroll = mBinding.nest.canScrollHorizontally(1)
        (mBinding.vp1.getChildAt(0) as? RecyclerView)?.setNestedScrollingEnabled(false)
        (mBinding.vp2.getChildAt(0) as? RecyclerView)?.setNestedScrollingEnabled(false)

        mBinding.nest.post {
            mBinding.vp2.updateLayoutParams {
                height = getScreenSize().height
            }
        }
    }

    fun getScreenSize(): Size {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
//            获取的是实际显示区域指定包含系统装饰的内容的显示部分
            val width = windowManager.currentWindowMetrics.bounds.width()
            val height = windowManager.currentWindowMetrics.bounds.height()
            Size(width, height)
//            Log.e(TAG, "width: $width,height:$height") //720,1491
//            val insets: Insets = windowManager.currentWindowMetrics.windowInsets
//                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
//            Log.e(TAG, "去掉任何系统栏的宽度:" + (width - insets.right - insets.left) + ",去掉任何系统栏的高度:" + (height - insets.bottom - insets.top))
        } else {
            //获取减去系统栏的屏幕的高度和宽度
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            Size(width, height)
//            Log.e(TAG, "width: $width,height:$height") //720,1491
        }
    }

}

class Adapter(val activity: FragmentActivity, val list: List<Fragment>) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return list[position]
    }

}