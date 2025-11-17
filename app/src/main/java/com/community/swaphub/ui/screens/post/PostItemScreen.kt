package com.community.swaphub.ui.screens.post

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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.community.swaphub.data.model.ItemType
import com.community.swaphub.viewmodel.ItemViewModel
import androidx.compose.ui.Alignment
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItemScreen(
    onBackClick: () -> Unit,
    onPostSuccess: () -> Unit,
    viewModel: ItemViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(ItemType.GIVEAWAY) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ---------------------------
    // IMAGE PICKERS
    // ---------------------------
    val pickVisualMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    val getContent = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) selectedImageUri = uri
    }

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

            // ------------------------------------
            // IMAGE PREVIEW
            // ------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No image selected", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ------------------------------------
            // TWO PICKER BUTTONS
            // ------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        pickVisualMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Photo Picker")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { getContent.launch("image/*") }
                ) {
                    Text("Gallery")
                }
            }

            // ------------------------------------
            // BASIC INPUTS
            // ------------------------------------
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

            // ------------------------------------
            // CATEGORY (REAL FLOW ROW)
            // ------------------------------------
            Text("Category")

            FlowRow(
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf(
                    "BOOKS",
                    "TOYS",
                    "CLOTHES",
                    "ELECTRONICS",
                    "FURNITURE",
                    "KITCHEN",
                    "SPORTS",
                    "OTHER",
                    "HOME DECOR",
                    "GADGETS",
                    "ACCESSORIES",
                    "STATIONERY",
                    "BEAUTY",
                    "OUTDOOR",
                    "KIDS",
                    "PETS"
                )

                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            // ------------------------------------
            // TYPE
            // ------------------------------------
            Text("Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemType.values().forEach { t ->
                    FilterChip(
                        selected = selectedType == t,
                        onClick = { selectedType = t },
                        label = { Text(t.name) }
                    )
                }
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState is com.community.swaphub.viewmodel.ItemUiState.Error) {
                Text(
                    text = (uiState as com.community.swaphub.viewmodel.ItemUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    viewModel.postItemWithImage(
                        title = title,
                        description = description,
                        category = selectedCategory,
                        type = selectedType,
                        location = location,
                        imageUri = selectedImageUri
                    )
                },
                enabled = title.isNotBlank() && selectedCategory != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Post Item")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
