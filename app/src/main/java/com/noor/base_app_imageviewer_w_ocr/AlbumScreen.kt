package com.noor.base_app_imageviewer_w_ocr

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

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
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            },
            actions = {
                if (isSelectionMode && selectedImages.isNotEmpty()) {
                    IconButton(onClick = { showTagSheet = true }) {
                        Icon(Icons.Default.Label, "Tag")
                    }
                }
                IconButton(onClick = { showSortSheet = true }) {
                    Icon(Icons.Default.Sort, "Sort")
                }
            }
        )

        // Content
        if (selectedFolder == null) {
            FolderGrid(
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

@Composable
fun FolderGrid(
    folders: List<ImageFolder>,
    onFolderClick: (ImageFolder) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders) { folder ->
            FolderItem(
                folder = folder,
                onClick = { onFolderClick(folder) }
            )
        }
    }
}

@Composable
fun FolderItem(
    folder: ImageFolder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(folder.coverImage?.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.3f)
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${folder.images.size} images",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ImageGrid(
    images: List<ImageItem>,
    selectedImages: Set<Long> = emptySet(),
    isSelectionMode: Boolean = false,
    onImageClick: (Int) -> Unit,
    onImageLongClick: ((Int) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(images) { index, image ->
            ImageGridItem(
                image = image,
                isSelected = selectedImages.contains(image.id),
                isSelectionMode = isSelectionMode,
                onClick = { onImageClick(index) },
                onLongClick = { onImageLongClick?.invoke(index) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGridItem(
    image: ImageItem,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Selection overlay
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else
                            Color.Black.copy(alpha = 0.2f)
                    )
            )

            // Selection indicator
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }

        // Tags indicator
        if (image.tags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                image.tags.take(3).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                tag.color.color,
                                shape = CircleShape
                            )
                    )
                }
                if (image.tags.size > 3) {
                    Text(
                        text = "+${image.tags.size - 3}",
                        color = Color.White,
                        fontSize = 8.sp,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TagSelectionSheet(
    availableTags: List<ImageTag>,
    onTagSelected: (ImageTag) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Add Tag",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(availableTags) { tag ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagSelected(tag) },
                    colors = CardDefaults.cardColors(
                        containerColor = tag.color.color.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    tag.color.color,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tag.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SortBottomSheet(
    currentSort: SortType,
    onSortSelected: (SortType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Sort by",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SortType.values().forEach { sortType ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSortSelected(sortType) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentSort == sortType,
                    onClick = { onSortSelected(sortType) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (sortType) {
                        SortType.DATE_ASCENDING -> "Date (Oldest first)"
                        SortType.DATE_DESCENDING -> "Date (Newest first)"
                        SortType.SIZE_ASCENDING -> "Size (Smallest first)"
                        SortType.SIZE_DESCENDING -> "Size (Largest first)"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
