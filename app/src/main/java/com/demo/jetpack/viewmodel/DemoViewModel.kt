package com.demo.jetpack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.demo.jetpack.core.data.remote.Repo
import com.demo.jetpack.core.data.repository.Repository
import kotlinx.coroutines.flow.Flow

class DemoViewModel : ViewModel() {

    fun getPagingData(): Flow<PagingData<Repo>> {
        return Repository.getPagingData().cachedIn(viewModelScope)
    }

}