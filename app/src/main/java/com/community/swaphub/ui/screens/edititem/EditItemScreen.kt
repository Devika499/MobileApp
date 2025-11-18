package com.community.swaphub.ui.screens.edititem

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.community.swaphub.data.model.ItemType
import com.community.swaphub.viewmodel.EditItemViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
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
    val scope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // IMAGE PICKER
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri
    }

    // PERMISSION
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted)
            Toast.makeText(context, "Location permission is required", Toast.LENGTH_LONG).show()
    }

    // LOAD ITEM
    LaunchedEffect(itemId) { viewModel.loadItem(itemId) }

    // REDIRECTS
    LaunchedEffect(uiState.isSuccess) { if (uiState.isSuccess) onUpdateSuccess() }
    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onDeleteSuccess() }

    // FORM STATE
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ItemType.SWAP) }
    var location by remember { mutableStateOf("") }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    // Fill values from backend
    LaunchedEffect(uiState.item) {
        uiState.item?.let { item ->
            title = item.title
            description = item.description ?: ""
            category = item.category ?: ""
            type = item.type ?: ItemType.SWAP
            location = item.location ?: ""
            latitude = item.latitude
            longitude = item.longitude
        }
    }

    val existingImageUrl = uiState.item?.imageUrl?.replace("localhost", "10.0.2.2")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteItem() }) {
                        Icon(Icons.Default.Archive, contentDescription = "Delete")
                    }
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

            // IMAGE PREVIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    ),
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

            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Change Picture")
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Type", style = MaterialTheme.typography.labelMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == ItemType.SWAP,
                    onClick = { type = ItemType.SWAP },
                    label = { Text("Swap") }
                )
                FilterChip(
                    selected = type == ItemType.GIVEAWAY,
                    onClick = { type = ItemType.GIVEAWAY },
                    label = { Text("Giveaway") }
                )
            }

            // AUTO LOCATION BUTTON
            Button(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

                    val fused = LocationServices.getFusedLocationProviderClient(context)

                    scope.launch {
                        val loc = fused.lastLocation.await()
                        if (loc != null) {
                            latitude = loc.latitude
                            longitude = loc.longitude
                            location = "Lat: ${loc.latitude}, Lng: ${loc.longitude}"
                            Toast.makeText(context, "Location Updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Unable to detect location", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use My Location")
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (auto/custom)") },
                modifier = Modifier.fillMaxWidth()
            )

            // UPDATE BUTTON
            Button(
                onClick = {
                    viewModel.updateItem(
                        title = title,
                        description = description,
                        category = category,
                        type = type,
                        location = location,
                        latitude = latitude,
                        longitude = longitude,
                        imageUri = selectedImageUri
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Item")
            }

            OutlinedButton(
                onClick = { viewModel.deleteItem() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Archive Item")
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/* -------------------------------------------------------
   REQUIRED await() EXTENSION — NO IMPORTS HERE
-------------------------------------------------------- */



suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resume(null) }
    }
