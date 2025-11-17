package com.community.swaphub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.community.swaphub.data.model.Item
import com.community.swaphub.data.model.ItemType
import com.community.swaphub.data.model.ItemStatus
import com.community.swaphub.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    private val _selectedItem = MutableStateFlow<Item?>(null)
    val selectedItem: StateFlow<Item?> = _selectedItem.asStateFlow()

    private val _uiState = MutableStateFlow<ItemUiState>(ItemUiState.Idle)
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    // ---------------------------------------------------------
    // LOAD "OTHERS" ITEMS (HomeScreen)
    // ---------------------------------------------------------
    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = ItemUiState.Loading

            itemRepository.getAvailableItems().collect { result ->
                result.onSuccess { list ->
                    _items.value = list
                    _uiState.value = ItemUiState.Success
                }.onFailure {
                    _uiState.value =
                        ItemUiState.Error(it.message ?: "Failed to load items")
                }
            }
        }
    }

    // ---------------------------------------------------------
    // POST ITEM + IMAGE
    // ---------------------------------------------------------
    fun postItemWithImage(
        title: String,
        description: String?,
        category: String?,
        type: ItemType,
        location: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.value = ItemUiState.Loading

            val result = itemRepository.postItemWithImage(
                title = title,
                description = description,
                category = category,
                type = type,
                location = location,
                latitude = latitude,
                longitude = longitude,
                imageUri = imageUri
            )

            _uiState.value =
                if (result.isSuccess) ItemUiState.Success
                else ItemUiState.Error(result.exceptionOrNull()?.message ?: "Failed to post item")
        }
    }

    // ---------------------------------------------------------
    // LOAD SPECIFIC ITEM
    // ---------------------------------------------------------
    fun loadItem(id: String) {
        viewModelScope.launch {
            _uiState.value = ItemUiState.Loading

            itemRepository.getItem(id).collect { result ->
                result.onSuccess {
                    _selectedItem.value = it
                    _uiState.value = ItemUiState.Success
                }.onFailure {
                    _uiState.value =
                        ItemUiState.Error(it.message ?: "Failed to load item")
                }
            }
        }
    }

    // ---------------------------------------------------------
    // DELETE ITEM
    // ---------------------------------------------------------
    fun deleteItem(id: String) {
        viewModelScope.launch {
            _uiState.value = ItemUiState.Loading
            val result = itemRepository.deleteItem(id)

            _uiState.value = if (result.isSuccess)
                ItemUiState.Success
            else
                ItemUiState.Error(result.exceptionOrNull()?.message ?: "Failed to delete item")
        }
    }

    // Add this function to ItemViewModel.kt
    fun loadNearbyItems(latitude: Double, longitude: Double, radiusKm: Double) {
        viewModelScope.launch {
            _uiState.value = ItemUiState.Loading

            itemRepository.getNearbyItems(latitude, longitude, radiusKm).collect { result ->
                result.onSuccess { list ->
                    _items.value = list
                    _uiState.value = ItemUiState.Success
                }.onFailure {
                    _uiState.value =
                        ItemUiState.Error(it.message ?: "Failed to load nearby items")
                }
            }
        }
    }

    // ---------------------------------------------------------
    // REQUEST SWAP
    // ---------------------------------------------------------
    fun requestSwap(itemId: String) {
        viewModelScope.launch {
            val currentItem = _selectedItem.value
            if (currentItem == null) {
                _uiState.value = ItemUiState.Error("Item not loaded yet")
                return@launch
            }

            _uiState.value = ItemUiState.Loading
            val result = itemRepository.updateItemStatus(currentItem, "PENDING_SWAP")

            _uiState.value = if (result.isSuccess) {
                loadItems()
                ItemUiState.Success
            } else {
                ItemUiState.Error(result.exceptionOrNull()?.message ?: "Failed to request item")
            }
        }
    }

    fun clearError() {
        _uiState.value = ItemUiState.Idle
    }
}

sealed class ItemUiState {
    object Idle : ItemUiState()
    object Loading : ItemUiState()
    object Success : ItemUiState()
    data class Error(val message: String) : ItemUiState()
}
