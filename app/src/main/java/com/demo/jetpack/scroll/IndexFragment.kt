package com.demo.jetpack.scroll

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.demo.jetpack.R
import com.demo.jetpack.common.BaseAdapter
import com.demo.jetpack.common.BaseListAdapter
import com.demo.jetpack.core.extension.viewBinding
import com.demo.jetpack.databinding.FragmentIndexBinding
import com.demo.jetpack.databinding.ItemIndexFragmentBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        ada.submitList(list)
        mBinding.rv.adapter = ada
//        mBinding.rv.setNestedScrollingEnabled(false);
//        lifecycleScope.launch {
//            while (true) {
//                delay(100)
//                mBinding.rv.setNestedScrollingEnabled(canScroll);
//            }
//        }
    }

    class Adapter : BaseAdapter<String, ItemIndexFragmentBinding>() {
        override fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup): ItemIndexFragmentBinding {
            return ItemIndexFragmentBinding.inflate(inflater, parent, false)
        }

        override fun ItemIndexFragmentBinding.onBindView(item: String, position: Int) {
            tvContent.text = item
        }

    }

}



