package com.community.swaphub.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.community.swaphub.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(
    onBackClick: () -> Unit,
    onChatClick: (otherUserId: String, itemId: String?) -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val conversations by chatViewModel.conversations.collectAsState()
    val currentUser by chatViewModel.currentUser.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val uiState by chatViewModel.uiState.collectAsState()

    // Load conversations when user is ready
    LaunchedEffect(currentUser) {
        currentUser?.let { chatViewModel.loadConversations() }
    }

    // -------------------------------------------------------------
    // 🔥 NEW: GROUPING LOGIC (ONLY UI-level, does not affect backend)
    // -------------------------------------------------------------
    val groupedConversations = remember(conversations) {
        conversations
            .groupBy { Pair(it.otherUserId, it.itemId) } // group per user + item
            .map { (_, convoList) ->
                convoList.maxByOrNull { it.lastMessageTime ?: "" } // latest message only
            }
            .filterNotNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversations") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (uiState is com.community.swaphub.viewmodel.ChatUiState.Error) {
                val msg = (uiState as com.community.swaphub.viewmodel.ChatUiState.Error).message
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Error: $msg", color = MaterialTheme.colorScheme.error)
                }
            }

            when {
                currentUser == null -> LoadingBox("Loading user information...")
                isLoading -> LoadingBox("Loading conversations...")
                groupedConversations.isEmpty() -> EmptyConversationsBox()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupedConversations) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    onChatClick(conversation.otherUserId, conversation.itemId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingBox(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(text)
        }
    }
}

@Composable
fun EmptyConversationsBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("No conversations yet")
            Text("Start chatting by requesting or accepting an item")
        }
    }
}

@Composable
fun ConversationItem(
    conversation: com.community.swaphub.data.model.ChatConversation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = "User")
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    conversation.otherUserName ?: "Unknown User",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                conversation.itemTitle?.let {
                    Text(
                        "Item: $it",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                conversation.lastMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            conversation.lastMessageTime?.let {
                Text(
                    formatTimestamp(it),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        if (timestamp.contains("T")) timestamp.substring(0, 10)
        else timestamp
    } catch (e: Exception) {
        timestamp
    }
}
