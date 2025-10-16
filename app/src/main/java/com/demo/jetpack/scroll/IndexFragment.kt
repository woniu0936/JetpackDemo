package com.demo.jetpack.scroll

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.demo.core.common.BaseAdapter
import com.demo.jetpack.R
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.FragmentIndexBinding
import com.demo.jetpack.databinding.ItemIndexFragmentBinding

class IndexFragment : Fragment(R.layout.fragment_index) {

    companion object {
        fun newInstance(index: Int): IndexFragment {
            return IndexFragment().apply {
                arguments = bundleOf("index" to index)
            }
        }
    }

    private val mBinding: FragmentIndexBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val index = arguments?.getInt("index")
        val list = (0..100).map { "index: $index-item: $it" }
        val ada = Adapter()
        ada.setData(list)
        mBinding.rv.adapter = ada
//        mBinding.rv.setNestedScrollingEnabled(false);
//        lifecycleScope.launch {
//            while (true) {
//                delay(100)
//                mBinding.rv.setNestedScrollingEnabled(canScroll);
//            }
//        }
    }

    class Adapter : BaseAdapter<String, ItemIndexFragmentBinding>(ItemIndexFragmentBinding::inflate) {

        override fun ItemIndexFragmentBinding.onBindView(item: String, position: Int) {
            tvContent.text = item
        }

    }

}



