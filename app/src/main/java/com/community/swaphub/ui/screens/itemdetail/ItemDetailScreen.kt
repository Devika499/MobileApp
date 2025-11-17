package com.community.swaphub.ui.screens.itemdetail

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.community.swaphub.data.model.ItemStatus
import com.community.swaphub.data.model.ItemType
import com.community.swaphub.viewmodel.ItemUiState
import com.community.swaphub.viewmodel.ItemViewModel
import com.community.swaphub.viewmodel.SwapRequestUiState
import com.community.swaphub.viewmodel.SwapRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: String,
    userId: String,
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit,
    viewModel: ItemViewModel = hiltViewModel(),
    swapViewModel: SwapRequestViewModel = hiltViewModel()
) {
    val item by viewModel.selectedItem.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val swapUiState by swapViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Debug logging to check values
    LaunchedEffect(itemId, userId) {
        println("DEBUG: ItemDetailScreen - itemId: $itemId, userId: $userId")
        viewModel.loadItem(itemId)
    }

    // Handle swap request state changes
    LaunchedEffect(swapUiState) {
        when (swapUiState) {
            is SwapRequestUiState.Success -> {
                Toast.makeText(context, "Swap request sent successfully!", Toast.LENGTH_SHORT).show()
                // Reload the item to update its status
                viewModel.loadItem(itemId)
                // Clear the success state
                swapViewModel.clearError()
            }
            is SwapRequestUiState.Error -> {
                val errorMessage = (swapUiState as SwapRequestUiState.Error).message
                Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                swapViewModel.clearError() // Clear error after showing
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is ItemUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is ItemUiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (uiState as ItemUiState.Error).message,
                    modifier = Modifier.padding(16.dp)
                )
            }

            else -> {
                item?.let { currentItem ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = currentItem.imageUrl ?: "https://via.placeholder.com/400"
                            ),
                            contentDescription = currentItem.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    currentItem.title,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Surface(
                                    color = when (currentItem.type) {
                                        ItemType.GIVEAWAY -> MaterialTheme.colorScheme.primaryContainer
                                        ItemType.SWAP -> MaterialTheme.colorScheme.secondaryContainer
                                        null -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = (currentItem.type ?: ItemType.SWAP).name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Category: ${currentItem.category ?: "N/A"}")
                            Text("Location: ${currentItem.location ?: "Not specified"}")

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Description", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(currentItem.description ?: "No description provided")

                            Spacer(modifier = Modifier.height(24.dp))

                            // Show status message
                            when (currentItem.status) {
                                ItemStatus.PENDING_SWAP -> {
                                    Text(
                                        "This item is already requested by someone.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                ItemStatus.SWAPPED -> {
                                    Text(
                                        "This item has been swapped.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                else -> {}
                            }

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        currentItem.ownerId?.let { ownerId ->
                                            if (ownerId.isNotBlank() && ownerId != "null") {
                                                onChatClick(ownerId)
                                            } else {
                                                Toast.makeText(context, "Cannot chat: Owner information missing", Toast.LENGTH_SHORT).show()
                                            }
                                        } ?: run {
                                            Toast.makeText(context, "Cannot chat: Owner information not available", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = currentItem.ownerId != null &&
                                            currentItem.ownerId.isNotBlank() &&
                                            currentItem.ownerId != "null"
                                ) {
                                    Text("Chat with Owner")
                                }

                                Button(
                                    onClick = {
                                        // Debug logging
                                        println("DEBUG: Requesting item - itemId: $itemId, userId: $userId")
                                        println("DEBUG: Current item ownerId: ${currentItem.ownerId}")

                                        if (itemId.isNotBlank() && userId.isNotBlank() &&
                                            itemId != "null" && userId != "null") {

                                            // Check if user is trying to request their own item
                                            if (currentItem.ownerId == userId) {
                                                Toast.makeText(context, "You cannot request your own item", Toast.LENGTH_LONG).show()
                                                return@Button
                                            }

                                            swapViewModel.createSwapRequest(
                                                requestedItemId = itemId,
                                                requesterId = userId
                                            )
                                        } else {
                                            val errorMsg = buildString {
                                                append("Cannot request item: ")
                                                if (itemId.isBlank() || itemId == "null") append("Invalid item ID ")
                                                if (userId.isBlank() || userId == "null") append("Invalid user ID")
                                            }
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = currentItem.status == ItemStatus.AVAILABLE &&
                                            swapUiState !is SwapRequestUiState.Loading &&
                                            currentItem.ownerId != userId // Can't request own item
                                ) {
                                    if (swapUiState is SwapRequestUiState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            if (currentItem.status == ItemStatus.PENDING_SWAP || currentItem.status == ItemStatus.SWAPPED)
                                                "Requested"
                                            else
                                                "Request for Item"
                                        )
                                    }
                                }
                            }

                            // Debug info (you can remove this in production)
                            if (itemId.isBlank() || userId.isBlank() || itemId == "null" || userId == "null") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Debug: itemId='$itemId', userId='$userId'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Item not found")
                    }
                }
            }
        }
    }
}