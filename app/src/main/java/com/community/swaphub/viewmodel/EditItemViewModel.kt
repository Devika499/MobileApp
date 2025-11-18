package com.community.swaphub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.community.swaphub.data.model.Item
import com.community.swaphub.data.model.ItemStatus
import com.community.swaphub.data.model.ItemType
import com.community.swaphub.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri

@HiltViewModel
class EditItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditItemUiState())
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                itemRepository.getItem(itemId).collect { result ->
                    result.onSuccess { item ->
                        _uiState.value = _uiState.value.copy(
                            item = item,
                            isLoading = false
                        )
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to load item",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load item",
                    isLoading = false
                )
            }
        }
    }

    fun updateItem(
        title: String,
        description: String,
        category: String,
        type: ItemType,
        location: String,
        latitude: Double?,       // ✅ Added
        longitude: Double?,      // ✅ Added
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            val itemId = _uiState.value.item?.id ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                itemRepository.updateItem(
                    itemId = itemId,
                    title = title,
                    description = description,
                    category = category,
                    type = type,
                    location = location,
                    latitude = latitude,        // ✅ Forwarded
                    longitude = longitude,      // ✅ Forwarded
                    imageUri = imageUri
                ).collect { result ->
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to update item",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update item",
                    isLoading = false
                )
            }
        }
    }

    fun deleteItem() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val item = _uiState.value.item ?: return@launch

            try {
                val result = itemRepository.updateItemStatus(item, ItemStatus.DELETED.name)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isDeleted = true
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to archive item",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to archive item",
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class EditItemUiState(
    val item: Item? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isDeleted: Boolean = false
)
