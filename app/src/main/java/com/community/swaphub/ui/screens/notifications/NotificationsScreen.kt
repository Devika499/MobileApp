package com.community.swaphub.ui.screens.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.community.swaphub.viewmodel.NotificationsViewModel
import com.community.swaphub.viewmodel.SwapRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    notificationsViewModel: NotificationsViewModel = hiltViewModel(),
    swapRequestViewModel: SwapRequestViewModel = hiltViewModel()
) {
    val notifications by notificationsViewModel.notifications.collectAsState()
    val swapUiState by swapRequestViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        notificationsViewModel.loadNotifications()
    }

    // Handle swap request actions
    LaunchedEffect(swapUiState) {
        when (swapUiState) {
            is com.community.swaphub.viewmodel.SwapRequestUiState.Success -> {
                // Reload notifications after action
                notificationsViewModel.loadNotifications()
            }
            is com.community.swaphub.viewmodel.SwapRequestUiState.Error -> {
                val errorMessage = (swapUiState as com.community.swaphub.viewmodel.SwapRequestUiState.Error).message
                android.widget.Toast.makeText(context, "Error: $errorMessage", android.widget.Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No notifications yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { notificationsViewModel.markAsRead(notification.id) },
                        onAcceptSwap = { swapRequestId ->
                            swapRequestViewModel.acceptSwapRequest(swapRequestId)
                        },
                        onDeclineSwap = { swapRequestId ->
                            // Since backend doesn't have decline, we'll handle it differently
                            // You can either remove this or implement a custom solution
                            android.widget.Toast.makeText(context, "Decline feature not available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: com.community.swaphub.data.model.Notification,
    onMarkAsRead: () -> Unit,
    onAcceptSwap: (String) -> Unit,
    onDeclineSwap: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.read) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.createdAt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Show appropriate actions based on notification type
            when (notification.type) {
                "SWAP_REQUEST" -> {
                    // Show Accept/Decline buttons for swap requests
                    if (!notification.read && notification.swapRequestId != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onAcceptSwap(notification.swapRequestId)
                                    onMarkAsRead()
                                }
                            ) {
                                Text("Accept")
                            }
                            OutlinedButton(
                                onClick = {
                                    onDeclineSwap(notification.swapRequestId)
                                    onMarkAsRead()
                                }
                            ) {
                                Text("Decline")
                            }
                        }
                    } else if (!notification.read) {
                        // Fallback: Mark as Read button for unread notifications
                        Button(
                            onClick = onMarkAsRead,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Mark as Read")
                        }
                    }
                }
                else -> {
                    // Show Mark as Read for other notification types
                    if (!notification.read) {
                        Button(
                            onClick = onMarkAsRead,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Mark as Read")
                        }
                    }
                }
            }
        }
    }
}