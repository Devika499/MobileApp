package com.community.swaphub.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.community.swaphub.ui.components.CategoryChip
import com.community.swaphub.ui.components.ItemCard
import com.community.swaphub.viewmodel.ChatViewModel
import com.community.swaphub.viewmodel.ItemViewModel
import com.community.swaphub.viewmodel.SwapRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemClick: (String) -> Unit,
    onPostItemClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToConversations: () -> Unit,
    itemViewModel: ItemViewModel = hiltViewModel(),
    swapRequestViewModel: SwapRequestViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: com.community.swaphub.viewmodel.AuthViewModel = hiltViewModel()
) {
    val items by itemViewModel.items.collectAsState()
    val uiState by itemViewModel.uiState.collectAsState()
    val swapRequests by swapRequestViewModel.swapRequests.collectAsState()
    val authUser by authViewModel.currentUser.collectAsState()
    val chatCurrentUser by chatViewModel.currentUser.collectAsState()

    // State for the selected filter
    var selectedFilter by remember { mutableStateOf(HomeFilter.ITEMS_AVAILABLE) }

    // State for nearby items radius - use Float for Slider compatibility
    var selectedRadius by remember { mutableStateOf(10f) } // Default 10km as Float
    var showRadiusDialog by remember { mutableStateOf(false) }

    // Debug: Compare auth user and chat user
    LaunchedEffect(authUser, chatCurrentUser) {
        println("DEBUG: HomeScreen - Auth user: ${authUser?.id}, Chat user: ${chatCurrentUser?.id}")
    }

    // Refresh auth status when screen appears
    LaunchedEffect(Unit) {
        println("DEBUG: HomeScreen - LaunchedEffect started")
        println("DEBUG: HomeScreen - Refreshing auth status")
        authViewModel.refreshCurrentUserIfNeeded()
    }

    // Load data when screen appears or filter changes
    LaunchedEffect(selectedFilter) {
        when (selectedFilter) {
            HomeFilter.ITEMS_AVAILABLE -> {
                itemViewModel.loadItems()
            }
            HomeFilter.YOUR_REQUESTED_ITEMS -> {
                swapRequestViewModel.loadSwapRequests()
            }
            HomeFilter.NEARBY_ITEMS -> {
                // We'll load nearby items when radius is selected
            }
        }
    }

    // Radius selection dialog
    if (showRadiusDialog) {
        AlertDialog(
            onDismissRequest = { showRadiusDialog = false },
            title = {
                Text(
                    "Search Radius",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(
                        "Select how far to search for items:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Slider(
                        value = selectedRadius,
                        onValueChange = { selectedRadius = it },
                        valueRange = 1f..100f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${selectedRadius.toInt()} km radius",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showRadiusDialog = false
                        // Load nearby items with selected radius
                        val defaultLat = 9.9312 // Example: Kochi latitude
                        val defaultLng = 76.2673 // Example: Kochi longitude
                        itemViewModel.loadNearbyItems(defaultLat, defaultLng, selectedRadius.toDouble())
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Show Items")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRadiusDialog = false
                        selectedFilter = HomeFilter.ITEMS_AVAILABLE
                        itemViewModel.loadItems()
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Community Swap Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Chat button to show conversations list
                    IconButton(
                        onClick = onNavigateToConversations,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Conversations",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    // Notifications button
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    // Profile button
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPostItemClick,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.shadow(8.dp, shape = CircleShape)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Post Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Welcome Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(4.dp, shape = MaterialTheme.shapes.medium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Welcome back! 👋",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Discover amazing items to swap in your community",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Filter Options - Beautiful category chips
            Text(
                "Browse Items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    CategoryChip(
                        category = "All Items",
                        selected = selectedFilter == HomeFilter.ITEMS_AVAILABLE,
                        onClick = {
                            selectedFilter = HomeFilter.ITEMS_AVAILABLE
                            itemViewModel.loadItems()
                        },
                        icon = Icons.Filled.Explore
                    )
                }
                item {
                    CategoryChip(
                        category = "My Requests",
                        selected = selectedFilter == HomeFilter.YOUR_REQUESTED_ITEMS,
                        onClick = {
                            selectedFilter = HomeFilter.YOUR_REQUESTED_ITEMS
                            swapRequestViewModel.loadSwapRequests()
                        },
                        icon = Icons.Filled.SwapHoriz
                    )
                }
                item {
                    CategoryChip(
                        category = "Nearby",
                        selected = selectedFilter == HomeFilter.NEARBY_ITEMS,
                        onClick = {
                            selectedFilter = HomeFilter.NEARBY_ITEMS
                            showRadiusDialog = true
                        },
                        icon = Icons.Filled.LocationOn
                    )
                }
            }

            // Show current radius when in nearby mode
            if (selectedFilter == HomeFilter.NEARBY_ITEMS) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Showing items within ${selectedRadius.toInt()} km radius",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content based on selected filter
            when (selectedFilter) {
                HomeFilter.ITEMS_AVAILABLE -> {
                    ItemsAvailableContent(
                        items = items,
                        uiState = uiState,
                        onItemClick = onItemClick
                    )
                }
                HomeFilter.YOUR_REQUESTED_ITEMS -> {
                    YourRequestedItemsContent(
                        swapRequests = swapRequests,
                        onItemClick = onItemClick
                    )
                }
                HomeFilter.NEARBY_ITEMS -> {
                    ItemsAvailableContent(
                        items = items,
                        uiState = uiState,
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}

@Composable
fun ItemsAvailableContent(
    items: List<com.community.swaphub.data.model.Item>,
    uiState: com.community.swaphub.viewmodel.ItemUiState,
    onItemClick: (String) -> Unit
) {
    when (uiState) {
        is com.community.swaphub.viewmodel.ItemUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Loading items...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is com.community.swaphub.viewmodel.ItemUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Explore,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
        else -> {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.Explore,
                            contentDescription = "No items",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "No items available",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Be the first to post an item!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { item ->
                        ItemCard(
                            item = item,
                            onClick = { onItemClick(item.id) },
                            modifier = Modifier.shadow(2.dp, shape = MaterialTheme.shapes.medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YourRequestedItemsContent(
    swapRequests: List<com.community.swaphub.data.model.SwapRequest>,
    onItemClick: (String) -> Unit
) {
    if (swapRequests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Filled.SwapHoriz,
                    contentDescription = "No requests",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "No requested items",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Items you've requested will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(swapRequests) { swapRequest ->
                // Create a temporary item from swap request for display
                val tempItem = com.community.swaphub.data.model.Item(
                    id = swapRequest.requestedItemId,
                    title = swapRequest.requestedItemTitle ?: "Requested Item",
                    description = "Status: ${swapRequest.status}",
                    category = null,
                    type = null,
                    location = null,
                    latitude = null,
                    longitude = null,
                    imageUrl = null,
                    ownerId = swapRequest.ownerId,
                    status = when (swapRequest.status) {
                        com.community.swaphub.data.model.SwapRequestStatus.REQUESTED ->
                            com.community.swaphub.data.model.ItemStatus.PENDING_SWAP
                        com.community.swaphub.data.model.SwapRequestStatus.ACCEPTED ->
                            com.community.swaphub.data.model.ItemStatus.PENDING_SWAP
                        com.community.swaphub.data.model.SwapRequestStatus.COMPLETED ->
                            com.community.swaphub.data.model.ItemStatus.SWAPPED
                        com.community.swaphub.data.model.SwapRequestStatus.REJECTED ->
                            com.community.swaphub.data.model.ItemStatus.AVAILABLE
                    }
                )

                ItemCard(
                    item = tempItem,
                    onClick = { onItemClick(swapRequest.requestedItemId) },
                    modifier = Modifier.shadow(2.dp, shape = MaterialTheme.shapes.medium)
                )
            }
        }
    }
}

// Update the enum to include nearby items
enum class HomeFilter {
    ITEMS_AVAILABLE,
    YOUR_REQUESTED_ITEMS,
    NEARBY_ITEMS
}