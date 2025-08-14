package com.demo.jetpack.paging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.demo.jetpack.core.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PagingViewModel @Inject constructor(private val mRepository: Repository) : ViewModel() {

    private val _selectedRepoIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _deletedRepoIds = MutableStateFlow<Set<Int>>(emptySet())

    val pagingDataFlow: Flow<PagingData<RepoItem>> = combine(
        mRepository.getPagingData().cachedIn(viewModelScope),
        _deletedRepoIds,
        _selectedRepoIds
    ) { pagingData, deletedIds, selectedIds ->
        pagingData
            .filter { repo -> repo.id !in deletedIds }
            .map { repo -> RepoItem(repo, isSelected = repo.id in selectedIds) }
    }

    fun toggleSelection(repoId: Int) {
        _selectedRepoIds.update { currentSelection ->
            if (repoId in currentSelection) {
                currentSelection - repoId
            } else {
                currentSelection + repoId
            }
        }
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val selectedIds = _selectedRepoIds.value
            _deletedRepoIds.update { it + selectedIds }
            _selectedRepoIds.value = emptySet()
        }
    }
}