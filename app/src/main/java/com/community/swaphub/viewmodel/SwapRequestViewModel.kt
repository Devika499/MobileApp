package com.community.swaphub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.community.swaphub.data.model.SwapRequest
import com.community.swaphub.data.repository.SwapRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwapRequestViewModel @Inject constructor(
    private val swapRequestRepository: SwapRequestRepository
) : ViewModel() {

    private val _swapRequests = MutableStateFlow<List<SwapRequest>>(emptyList())
    val swapRequests: StateFlow<List<SwapRequest>> = _swapRequests.asStateFlow()

    private val _myRequestedItems = MutableStateFlow<List<SwapRequest>>(emptyList())
    val myRequestedItems: StateFlow<List<SwapRequest>> = _myRequestedItems.asStateFlow()

    private val _uiState = MutableStateFlow<SwapRequestUiState>(SwapRequestUiState.Idle)
    val uiState: StateFlow<SwapRequestUiState> = _uiState.asStateFlow()

    fun loadSwapRequests() {
        viewModelScope.launch {
            _uiState.value = SwapRequestUiState.Loading
            swapRequestRepository.getMySwapRequests().collect { result ->
                result.onSuccess { requests ->
                    _swapRequests.value = requests
                    _uiState.value = SwapRequestUiState.Success
                }.onFailure { error ->
                    _uiState.value = SwapRequestUiState.Error(error.message ?: "Failed to load swap requests")
                }
            }
        }
    }

    // For home screen - gets only items requested by the current user
    fun loadMyRequestedItems() {
        viewModelScope.launch {
            _uiState.value = SwapRequestUiState.Loading
            swapRequestRepository.getMyRequestedItems().collect { result ->
                result.onSuccess { requests ->
                    _myRequestedItems.value = requests
                    _uiState.value = SwapRequestUiState.Success
                }.onFailure { error ->
                    _uiState.value = SwapRequestUiState.Error(error.message ?: "Failed to load your requested items")
                }
            }
        }
    }

    fun createSwapRequest(requestedItemId: String, requesterId: String) {
        viewModelScope.launch {
            _uiState.value = SwapRequestUiState.Loading

            if (requestedItemId.isBlank() || requesterId.isBlank()) {
                _uiState.value = SwapRequestUiState.Error("Cannot create swap request: Invalid item or user ID")
                return@launch
            }

            val result = swapRequestRepository.createSwapRequest(requestedItemId, requesterId)
            _uiState.value = if (result.isSuccess) {
                SwapRequestUiState.Success
            } else {
                SwapRequestUiState.Error(result.exceptionOrNull()?.message ?: "Failed to create swap request")
            }
        }
    }

    fun acceptSwapRequest(id: String) {
        viewModelScope.launch {
            _uiState.value = SwapRequestUiState.Loading
            val result = swapRequestRepository.acceptSwapRequest(id)
            _uiState.value = if (result.isSuccess) SwapRequestUiState.Success
            else SwapRequestUiState.Error(result.exceptionOrNull()?.message ?: "Failed to accept swap request")
        }
    }

    fun declineSwapRequest(id: String) {
        viewModelScope.launch {
            _uiState.value = SwapRequestUiState.Loading
            val result = swapRequestRepository.declineSwapRequest(id)
            _uiState.value = if (result.isSuccess) SwapRequestUiState.Success
            else SwapRequestUiState.Error(result.exceptionOrNull()?.message ?: "Failed to decline swap request")
        }
    }

    fun completeSwapRequest(id: String) {
        viewModelScope.launch {
            _uiState.value = SwapRequestUiState.Loading
            val result = swapRequestRepository.completeSwapRequest(id)
            _uiState.value = if (result.isSuccess) SwapRequestUiState.Success
            else SwapRequestUiState.Error(result.exceptionOrNull()?.message ?: "Failed to complete swap request")
        }
    }

    fun clearError() {
        _uiState.value = SwapRequestUiState.Idle
    }
}

sealed class SwapRequestUiState {
    object Idle : SwapRequestUiState()
    object Loading : SwapRequestUiState()
    object Success : SwapRequestUiState()
    data class Error(val message: String) : SwapRequestUiState()
}