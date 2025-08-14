package com.demo.jetpack.paging

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.demo.jetpack.core.extension.viewBindings
import com.demo.jetpack.databinding.ActivityDemoBinding
import com.demo.jetpack.databinding.ActivityDemoBinding.inflate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PagingActivity : AppCompatActivity() {

    private val mBinding: ActivityDemoBinding by viewBindings(::inflate)
    private val mViewModel: PagingViewModel by viewModels()

    @Inject
    lateinit var mAdapter: PagingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        initView()
        fetchData()
    }

    private fun initView() = with(mBinding) {
        recyclerView.layoutManager = LinearLayoutManager(this@PagingActivity)
        recyclerView.adapter = mAdapter.withLoadStateFooter(PagingFooterAdapter { mAdapter.retry() })

        mAdapter.onItemClick = { repoItem ->
            mViewModel.toggleSelection(repoItem.repo.id)
        }

        deleteButton.setOnClickListener {
            mViewModel.deleteSelectedItems()
        }

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
                    Toast.makeText(this@PagingActivity, "Load Error: ${state.error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchData() {
        lifecycleScope.launch {
            mViewModel.pagingDataFlow.collectLatest { pagingData ->
                mAdapter.submitData(pagingData)
            }
        }
    }

}