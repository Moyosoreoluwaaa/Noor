package com.noor.base_app_imageviewer_w_ocr.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.noor.base_app_imageviewer_w_ocr.ImageFolder
import com.noor.base_app_imageviewer_w_ocr.ImageRepository
import com.noor.base_app_imageviewer_w_ocr.SortType
import com.noor.base_app_imageviewer_w_ocr.TagRepository
import com.noor.base_app_imageviewer_w_ocr.presentation.components.ImageFolderGrid
import com.noor.base_app_imageviewer_w_ocr.presentation.components.ImageGrid
import com.noor.base_app_imageviewer_w_ocr.presentation.components.SortBottomSheet
import com.noor.base_app_imageviewer_w_ocr.presentation.components.TagSelectionSheet
import com.noor.base_app_imageviewer_w_ocr.sortImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    onImageClick: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val imageRepository = remember { ImageRepository(context) }
    val tagRepository = remember { TagRepository(context) }

    var folders by remember { mutableStateOf<List<ImageFolder>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<ImageFolder?>(null) }
    var sortType by remember { mutableStateOf(SortType.DATE_DESCENDING) }
    var showSortSheet by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        folders = imageRepository.getAllImageFolders()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = when {
                        isSelectionMode -> "${selectedImages.size} selected"
                        selectedFolder != null -> selectedFolder!!.name
                        else -> "Noor"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                if (selectedFolder != null || isSelectionMode) {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            isSelectionMode = false
                            selectedImages = emptySet()
                        } else {
                            selectedFolder = null
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            },
            actions = {
                if (isSelectionMode && selectedImages.isNotEmpty()) {
                    IconButton(onClick = { showTagSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.Label, "Tag")
                    }
                }
                IconButton(onClick = { showSortSheet = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                }
            }
        )

        // Content
        if (selectedFolder == null) {
            ImageFolderGrid(
                folders = folders,
                onFolderClick = { selectedFolder = it }
            )
        } else {
            ImageGrid(
                images = selectedFolder!!.images.sortImages(sortType),
                selectedImages = selectedImages,
                isSelectionMode = isSelectionMode,
                onImageClick = { index ->
                    if (isSelectionMode) {
                        val image = selectedFolder!!.images[index]
                        selectedImages = if (selectedImages.contains(image.id)) {
                            selectedImages - image.id
                        } else {
                            selectedImages + image.id
                        }
                    } else {
                        onImageClick(index, selectedFolder!!.path)
                    }
                },
                onImageLongClick = { index ->
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        val image = selectedFolder!!.images[index]
                        selectedImages = setOf(image.id)
                    }
                }
            )
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false }
        ) {
            SortBottomSheet(
                currentSort = sortType,
                onSortSelected = {
                    sortType = it
                    showSortSheet = false
                }
            )
        }
    }

    // Tag Bottom Sheet
    if (showTagSheet && isSelectionMode) {
        ModalBottomSheet(
            onDismissRequest = { showTagSheet = false }
        ) {
            TagSelectionSheet(
                availableTags = tagRepository.getAllAvailableTags(),
                onTagSelected = { tag ->
                    selectedImages.forEach { imageId ->
                        tagRepository.addTagToImage(imageId, tag)
                    }
                    // Refresh folder data
                    folders = imageRepository.getAllImageFolders()
                    selectedFolder = folders.find { it.path == selectedFolder?.path }
                    showTagSheet = false
                }
            )
        }
    }
}
