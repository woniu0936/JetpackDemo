package com.demo.jetpack.viewmodel

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.demo.jetpack.extension.viewBindings
import com.demo.jetpack.databinding.ActivityDemoBinding
import com.demo.jetpack.databinding.ActivityDemoBinding.inflate
import kotlinx.coroutines.launch

class DemoActivity : AppCompatActivity() {

    private val mBinding: ActivityDemoBinding by viewBindings(::inflate)
    private val mViewModel: DemoViewModel by viewModels()
    private val mAdapter: DemoAdapter by lazy { DemoAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        initView()
        fetchData()
    }

    private fun initView() = with(mBinding) {
        recyclerView.layoutManager = LinearLayoutManager(this@DemoActivity)
        recyclerView.adapter = mAdapter.withLoadStateFooter(FooterAdapter { mAdapter.retry() })

        mAdapter.addLoadStateListener {
            when (it.refresh) {
                is LoadState.NotLoading -> {
                    progressBar.visibility = View.INVISIBLE
                    recyclerView.visibility = View.VISIBLE
                }

                is LoadState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.INVISIBLE
                }

                is LoadState.Error -> {
                    val state = it.refresh as LoadState.Error
                    progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this@DemoActivity, "Load Error: ${state.error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchData() {
        lifecycleScope.launch {
            mViewModel.getPagingData().collect { pagingData ->
                mAdapter.submitData(pagingData)
            }
        }
    }

}