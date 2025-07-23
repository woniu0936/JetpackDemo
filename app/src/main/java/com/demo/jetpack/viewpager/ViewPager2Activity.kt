package com.demo.jetpack.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.demo.jetpack.R
import com.demo.jetpack.common.BaseAdapter
import com.demo.jetpack.core.extension.viewBindings
import com.demo.jetpack.databinding.ActivityViewPager2Binding
import com.demo.jetpack.databinding.ItemViewPager2Binding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViewPager2Activity : AppCompatActivity() {

    private val mBinding: ActivityViewPager2Binding by viewBindings(ActivityViewPager2Binding::inflate)

    @Inject
    lateinit var mGalleryTransformer: GalleryTransformer

    lateinit var mAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        mAdapter = ViewPagerAdapter()

        mBinding.viewPager01.apply {
            adapter = mAdapter
            offscreenPageLimit = 3
            setPageTransformer(mGalleryTransformer)
            (getChildAt(0) as? RecyclerView)?.apply {
                setPadding(200, 0, 200, 0)
                // 画廊效果的关键是这行代码
                clipToPadding = false
            }
        }
        mBinding.viewPager02.apply {
            adapter = mAdapter
            offscreenPageLimit = 3
            setPageTransformer(SliderTransformer(3))
            (getChildAt(0) as? RecyclerView)?.apply {
                setPadding(100, 0, 100, 0)
                // 让子view超过ViewPager2(准确的说是ViewPager2中的RecyclerView)的边界的关键是这行代码
                clipToPadding = false
            }
        }

        mBinding.viewPager03.apply {
            adapter = mAdapter
            offscreenPageLimit = 3
            setPageTransformer(CarouselPageTransformer(24))
            (getChildAt(0) as? RecyclerView)?.apply {
                setPadding(100, 0, 640, 0)
                // 让子view超过ViewPager2(准确的说是ViewPager2中的RecyclerView)的边界的关键是这行代码
                clipToPadding = false
            }
        }
        mAdapter.submitList(defaultList())
    }

    private fun defaultList(): List<String> {
        return 0.until(80).map { "$it" }
    }

}

val colorsRes = listOf(
    R.color.color_f1707d,
    R.color.color_f1f1b8,
    R.color.color_ef5767,
    R.color.color_ddff95,
    R.color.color_ae716e,
    R.color.color_b8f1cc,
    R.color.color_f1b8f1,
    R.color.color_e96d29,
    R.color.color_ac5e74,
    R.color.color_fd803a
)

class ViewPagerAdapter : BaseAdapter<String, ItemViewPager2Binding>() {

    override fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup): ItemViewPager2Binding {
        return ItemViewPager2Binding.inflate(inflater, parent, false)
    }

    override fun ItemViewPager2Binding.onBindView(item: String, position: Int) {
        tvIndex.text = item
        root.setBackgroundResource(colorsRes[position % colorsRes.size])
    }

}
