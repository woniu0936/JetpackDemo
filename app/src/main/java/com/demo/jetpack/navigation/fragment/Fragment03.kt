package com.demo.jetpack.navigation.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.demo.jetpack.R
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.FragmentNavigation03Binding

class Fragment03 : Fragment(R.layout.fragment_navigation_03) {

    private val mBinding: FragmentNavigation03Binding by viewBinding()
    private val navArgs: Fragment03Args by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Toast.makeText(activity, "param: ${navArgs.param}", Toast.LENGTH_SHORT).show()
    }

}