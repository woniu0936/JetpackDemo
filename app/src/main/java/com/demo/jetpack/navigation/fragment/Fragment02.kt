package com.demo.jetpack.navigation.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.demo.jetpack.R
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.FragmentNavigation02Binding

class Fragment02 : Fragment(R.layout.fragment_navigation_02) {

    private val mBinding: FragmentNavigation02Binding by viewBinding()
    private val param by lazy { arguments?.getString("param") }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Toast.makeText(activity, "param: $param", Toast.LENGTH_SHORT).show()
    }

}