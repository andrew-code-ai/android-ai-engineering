package com.example.claims.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claims.domain.Claim
import com.example.claims.domain.ObserveClaimsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ClaimListUiState {
    data object Loading : ClaimListUiState
    data class Content(val claims: List<Claim>) : ClaimListUiState
}

@HiltViewModel
class ClaimListViewModel @Inject constructor(observeClaims: ObserveClaimsUseCase) : ViewModel() {
    val uiState: StateFlow<ClaimListUiState> = observeClaims()
        .map<List<Claim>, ClaimListUiState> { ClaimListUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClaimListUiState.Loading)
}
