package com.community.swaphub.ui.screens.post

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.community.swaphub.data.model.ItemType
import com.community.swaphub.viewmodel.ItemViewModel
import androidx.compose.ui.Alignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun PostItemScreen(
    onBackClick: () -> Unit,
    onPostSuccess: () -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(ItemType.GIVEAWAY) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // GPS values
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    val fusedLocation = LocationServices.getFusedLocationProviderClient(context)

    // LOCATION PERMISSION
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val loc = fusedLocation.lastLocation.await()
                        loc?.let {
                            latitude = it.latitude
                            longitude = it.longitude
                        }
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    // ============================================================
    // CAMERA PERMISSION + CAMERA LAUNCHER
    // ============================================================

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun createTempImageUri(): Uri {
        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = tempCameraUri
        }
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                tempCameraUri = createTempImageUri()
                cameraLauncher.launch(tempCameraUri!!)
            }
        }


    // ============================================================
    // GALLERY PICKERS
    // ============================================================

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) selectedImageUri = uri
    }


    // POST SUCCESS LISTENER
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState) {
        if (uiState is com.community.swaphub.viewmodel.ItemUiState.Success) {
            onPostSuccess()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Item") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // IMAGE PREVIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("No image selected")
                }
            }


            // PICKER BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Gallery
                OutlinedButton(
                    onClick = {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Gallery") }

                // Camera
                Button(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Camera") }
            }


            // TEXT INPUTS
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
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )


            // CATEGORY
            Text("Category")
            FlowRow(
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "BOOKS", "TOYS", "CLOTHES", "ELECTRONICS", "FURNITURE",
                    "KITCHEN", "SPORTS", "OTHER", "HOME DECOR", "GADGETS",
                    "ACCESSORIES", "STATIONERY", "BEAUTY", "OUTDOOR", "KIDS", "PETS"
                ).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }


            // TYPE
            Text("Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemType.values().forEach {
                    FilterChip(
                        selected = selectedType == it,
                        onClick = { selectedType = it },
                        label = { Text(it.name) }
                    )
                }
            }


            // LOCATION STATUS
            if (latitude == null) {
                Text("Fetching location...", color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Location detected ✓", color = MaterialTheme.colorScheme.primary)
            }


            // SUBMIT BUTTON
            Button(
                onClick = {
                    if (latitude != null && longitude != null) {
                        viewModel.postItemWithImage(
                            title = title,
                            description = description,
                            category = selectedCategory,
                            type = selectedType,
                            location = null,
                            latitude = latitude,
                            longitude = longitude,
                            imageUri = selectedImageUri
                        )
                    }
                },
                enabled = title.isNotBlank() && selectedCategory != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Post Item") }


            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


// AWAIT EXTENSION
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it, null) }
        addOnFailureListener { cont.resume(null, null) }
    }
