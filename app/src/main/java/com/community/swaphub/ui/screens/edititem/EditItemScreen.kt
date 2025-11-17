package com.community.swaphub.ui.screens.edititem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.community.swaphub.data.model.ItemType
import com.community.swaphub.viewmodel.EditItemViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: String,
    onBackClick: () -> Unit,
    onUpdateSuccess: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val viewModel: EditItemViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri
    }

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    // Redirects
    LaunchedEffect(uiState.isSuccess) { if (uiState.isSuccess) onUpdateSuccess() }
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onDeleteSuccess() }

    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ItemType.SWAP) }
    var location by remember { mutableStateOf("") }

    LaunchedEffect(uiState.item) {
        uiState.item?.let { item ->
            title = item.title
            description = item.description ?: ""
            category = item.category ?: ""
            type = item.type ?: ItemType.SWAP
            location = item.location ?: ""
        }
    }

    val existingImageUrl = uiState.item?.imageUrl?.replace("localhost", "10.0.2.2")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteItem() }) { Icon(Icons.Default.Archive, contentDescription = "Archive") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image preview
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha=0.06f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selectedImageUri ?: existingImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Item image",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Change Picture")
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())

            Text("Type", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = type == ItemType.SWAP, onClick = { type = ItemType.SWAP }, label = { Text("Swap") })
                FilterChip(selected = type == ItemType.GIVEAWAY, onClick = { type = ItemType.GIVEAWAY }, label = { Text("Giveaway") })
            }

            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())

            Button(onClick = {
                viewModel.updateItem(
                    title = title,
                    description = description,
                    category = category,
                    type = type,
                    location = location,
                    imageUri = selectedImageUri
                )
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Update Item")
            }

            OutlinedButton(onClick = { viewModel.deleteItem() }, modifier = Modifier.fillMaxWidth()) {
                Text("Archive Item")
            }

            uiState.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
