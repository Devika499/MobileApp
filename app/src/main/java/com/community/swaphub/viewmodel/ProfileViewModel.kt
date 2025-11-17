package com.community.swaphub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.community.swaphub.data.model.Item
import com.community.swaphub.data.model.ItemStatus
import com.community.swaphub.data.model.PointsHistory
import com.community.swaphub.data.model.User
import com.community.swaphub.data.repository.AuthRepository
import com.community.swaphub.data.repository.ItemRepository
import com.community.swaphub.data.repository.PointsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val pointsRepository: PointsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userItems = MutableStateFlow<List<Item>>(emptyList())
    val userItems: StateFlow<List<Item>> = _userItems.asStateFlow()

    private val _allUserItems = MutableStateFlow<List<Item>>(emptyList())
    val allUserItems: StateFlow<List<Item>> = _allUserItems.asStateFlow()

    private val _points = MutableStateFlow(0)
    val points: StateFlow<Int> = _points.asStateFlow()

    private val _pointsHistory = MutableStateFlow<List<PointsHistory>>(emptyList())
    val pointsHistory: StateFlow<List<PointsHistory>> = _pointsHistory.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // ---------------- UPDATE USER PROFILE ----------------
    fun updateUserDetails(updatedUser: User) {
        viewModelScope.launch {
            val result = authRepository.updateUser(updatedUser)

            result.onSuccess {
                authRepository.refreshCurrentUserIfNeeded()
            }.onFailure { error ->
                println("Failed to update user: ${error.message}")
            }
        }
    }

    // ---------------- LOAD USER ITEMS ----------------
    fun loadMyItems() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            itemRepository.getMyItems().collect { result ->
                result.onSuccess { items ->
                    _allUserItems.value = items

                    val activeItems = items.filter {
                        it.status != ItemStatus.DELETED && it.status != ItemStatus.SWAPPED
                    }

                    _userItems.value = activeItems
                    _uiState.value = ProfileUiState.Success
                }.onFailure { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Error loading items")
                }
            }
        }
    }

    fun loadPoints(userId: String) {
        viewModelScope.launch {
            pointsRepository.getUserPoints(userId).collect { result ->
                result.onSuccess { _points.value = it }
            }
        }
    }

    fun loadPointsHistory(userId: String) {
        viewModelScope.launch {
            pointsRepository.getPointsHistory(userId).collect { result ->
                result.onSuccess { _pointsHistory.value = it }
            }
        }
    }

    fun clearError() {
        _uiState.value = ProfileUiState.Idle
    }
}

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    object Success : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
