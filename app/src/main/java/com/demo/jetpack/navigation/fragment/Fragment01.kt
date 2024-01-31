package com.demo.jetpack.navigation.fragment

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.demo.jetpack.R
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.FragmentNavigation01Binding

class Fragment01 : Fragment(R.layout.fragment_navigation_01) {

    private val mBinding: FragmentNavigation01Binding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.tvNavFragment.setOnClickListener {
            findNavController().navigate(R.id.fragment02, bundleOf("param" to "hello"))
        }

        mBinding.tvNavFragment03.setOnClickListener {
            Fragment01Directions.toFragment03("hello fragment03").run {
                findNavController().navigate(actionId, arguments)
            }
        }

        mBinding.tvNavActivity.setOnClickListener {
            findNavController().navigate(R.id.new_activity)
        }
    }

}