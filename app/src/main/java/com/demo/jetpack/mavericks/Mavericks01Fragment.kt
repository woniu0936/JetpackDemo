package com.demo.jetpack.mavericks

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.demo.jetpack.R
import com.demo.jetpack.databinding.FragmentMavericksDemo01Binding
import com.demo.jetpack.extension.logD
import com.demo.jetpack.extension.viewBinding

//[注意]MavericksView不支持activity，原因是google官方和Airbnb都更加推荐使用fragment承载页面你的ui，而不是activity
class Mavericks01Fragment : Fragment(R.layout.fragment_mavericks_demo_01), MavericksView {

    private val mViewModel: Mavericks01ViewModel by fragmentViewModel()
    private val mBinding: FragmentMavericksDemo01Binding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initEvent()
    }

    private fun initEvent() {
        mBinding.tvCount.setOnClickListener {
            mViewModel.incrementCount()
            logD { "count view click" }
        }
    }

    override fun invalidate() = withState(mViewModel) { state ->
        mBinding.tvCount.text = "count:${state.count}"
        logD { "count view refresh" }
    }

}

//驱动UI的数据
data class CounterState(val count: Int = 0) : MavericksState

//等效官方的ViewModel
class Mavericks01ViewModel(initialState: CounterState) : MavericksViewModel<CounterState>(initialState) {

    fun incrementCount() = setState { copy(count = count + 1) }

}