package com.demo.jetpack.paging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.demo.jetpack.core.data.remote.Repo
import com.demo.jetpack.core.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class PagingViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var mRepository: Repository

    fun getPagingData(): Flow<PagingData<Repo>> {
        return mRepository.getPagingData().cachedIn(viewModelScope)
    }

}