package com.community.swaphub.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.community.swaphub.ui.components.ItemCard
import com.community.swaphub.viewmodel.AuthViewModel
import com.community.swaphub.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onItemClick: (String) -> Unit,
    onNavigateToNotifications: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val userItems by viewModel.userItems.collectAsState()
    val allUserItems by viewModel.allUserItems.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var showEditSheet by remember { mutableStateOf(false) }

    // Edit fields
    var editName by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        authViewModel.refreshCurrentUserIfNeeded()
        viewModel.loadMyItems()
        // ❌ Removed: loading points
    }

    // Bottom sheet for editing profile
    if (showEditSheet && currentUser != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit Profile", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editLocation,
                    onValueChange = { editLocation = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val updated = currentUser!!.copy(
                            name = editName,
                            location = editLocation
                        )

                        scope.launch {
                            viewModel.updateUserDetails(updated)
                            showEditSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadMyItems()
                        // ❌ Removed: points reload
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    onNavigateToNotifications?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Notifications, "Notifications")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // HEADER CARD (Points Removed)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = currentUser?.name ?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Location: ${currentUser?.location ?: "Not set"}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(12.dp))

                        // EDIT BUTTON
                        OutlinedButton(
                            onClick = {
                                editName = currentUser?.name ?: ""
                                editLocation = currentUser?.location ?: ""
                                showEditSheet = true
                                scope.launch { sheetState.show() }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Profile")
                        }
                    }
                }
            }

            item {
                Text(
                    "My Items",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            when (uiState) {
                is com.community.swaphub.viewmodel.ProfileUiState.Loading -> item {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    }
                }

                else -> {
                    if (userItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No active items.")
                            }
                        }
                    } else {
                        items(userItems) { item ->
                            ItemCard(
                                item = item,
                                onClick = { onItemClick(item.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
