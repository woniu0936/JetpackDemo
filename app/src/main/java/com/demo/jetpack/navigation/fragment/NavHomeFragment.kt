package com.demo.jetpack.navigation.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import com.demo.jetpack.R
import com.demo.jetpack.core.extension.logD
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.FragmentNavHomeBinding

class NavHomeFragment : Fragment(R.layout.fragment_nav_home) {

    private val TAG = NavHomeFragment::class.java.simpleName

    private val mBinding: FragmentNavHomeBinding by viewBinding()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logD(TAG) { "------------------------------onCreateView-----------------------------" }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logD(TAG) { "------------------------------onViewCreated-----------------------------" }
        mBinding.root.postDelayed(3000) {
            mBinding.tvContent.text = "hello home fragment"
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        logD(TAG) { "------------------------------onAttach-----------------------------" }
    }

    override fun onDetach() {
        super.onDetach()
        logD(TAG) { "------------------------------onDetach-----------------------------" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logD(TAG) { "------------------------------onCreate-----------------------------" }
    }

    override fun onStart() {
        super.onStart()
        logD(TAG) { "------------------------------onStart-----------------------------" }
    }

    override fun onResume() {
        super.onResume()
        logD(TAG) { "------------------------------onResume-----------------------------" }
    }

    override fun onPause() {
        super.onPause()
        logD(TAG) { "------------------------------onPause-----------------------------" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        logD(TAG) { "------------------------------onDestroyView-----------------------------" }
    }

    override fun onDestroy() {
        super.onDestroy()
        logD(TAG) { "------------------------------onDestroy-----------------------------" }
    }

}