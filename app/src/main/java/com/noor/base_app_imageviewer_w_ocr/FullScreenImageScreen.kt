package com.noor.base_app_imageviewer_w_ocr
// FullScreenImageScreen.kt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.noor.base_app_imageviewer_w_ocr.presentation.components.FullScreenTagSheet
import com.noor.base_app_imageviewer_w_ocr.presentation.components.performOCR
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun FullScreenImageScreen(
    initialIndex: Int,
    folderPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val imageRepository = remember { ImageRepository(context) }
    val tagRepository = remember { TagRepository(context) }
    val ocrProcessor = remember { OCRProcessor(context) }

    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var isTopBarVisible by remember { mutableStateOf(true) }
    var isFloatingToolbarVisible by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var extractedText by remember { mutableStateOf<String?>(null) }
    var isProcessingOCR by remember { mutableStateOf(false) }
    var showOCRResult by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }

    LaunchedEffect(folderPath) {
        images = imageRepository.getImagesInFolder(folderPath)
    }

    LaunchedEffect(Unit) {
        delay(3000)
        isTopBarVisible = false
        isFloatingToolbarVisible = true
    }

    // Auto OCR when image changes
    LaunchedEffect(currentIndex) {
        if (images.isNotEmpty()) {
            delay(1000)
            GlobalScope.launch {
                performOCR(
                    imageUri = images[currentIndex].uri,
                    ocrProcessor = ocrProcessor,
                    onResult = { text ->
                        extractedText = text
                        showOCRResult = text.isNotEmpty()
                    },
                    onProcessing = { isProcessing ->
                        isProcessingOCR = isProcessing
                    }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (images.isNotEmpty()) {
            val currentImage = images[currentIndex]

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentImage.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                isTopBarVisible = !isTopBarVisible
                                isFloatingToolbarVisible = !isFloatingToolbarVisible
                            },
                            onDoubleTap = {
                                scale = if (scale > 1f) 1f else 2f
                                if (scale == 1f) offset = Offset.Zero
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(0.5f, 4f)
                            scale = newScale

                            offset = if (scale > 1f) {
                                Offset(
                                    x = (offset.x + pan.x).coerceIn(
                                        -size.width * (scale - 1) / 2,
                                        size.width * (scale - 1) / 2
                                    ),
                                    y = (offset.y + pan.y).coerceIn(
                                        -size.height * (scale - 1) / 2,
                                        size.height * (scale - 1) / 2
                                    )
                                )
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (scale <= 1f) {
                                    if (offset.x > 100) {
                                        if (currentIndex > 0) {
                                            currentIndex--
                                            scale = 1f
                                            offset = Offset.Zero
                                        }
                                    } else if (offset.x < -100) {
                                        if (currentIndex < images.size - 1) {
                                            currentIndex++
                                            scale = 1f
                                            offset = Offset.Zero
                                        }
                                    }
                                    offset = Offset.Zero
                                }
                            }
                        ) { _, dragAmount ->
                            if (scale <= 1f) {
                                offset = Offset(offset.x + dragAmount.x, 0f)
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Top Bar with Animation
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    if (images.isNotEmpty()) {
                        Column {
                            Text(text = images[currentIndex].displayName)
                            if (images[currentIndex].tags.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    images[currentIndex].tags.take(5).forEach { tag ->
                                        Surface(
                                            color = tag.color.color,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(16.dp)
                                        ) {
                                            Text(
                                                text = tag.displayName,
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }

        // Floating Toolbar with Animation
        AnimatedVisibility(
            visible = isFloatingToolbarVisible,
            enter = slideInVertically(
                initialOffsetY = { it }
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it }
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // OCR Button
                    IconButton(
                        onClick = {
                            if (images.isNotEmpty()) {
                                GlobalScope.launch {
                                    performOCR(
                                        imageUri = images[currentIndex].uri,
                                        ocrProcessor = ocrProcessor,
                                        onResult = { text ->
                                            extractedText = text
                                            showOCRResult = text.isNotEmpty()
                                        },
                                        onProcessing = { isProcessing ->
                                            isProcessingOCR = isProcessing
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isProcessingOCR
                    ) {
                        if (isProcessingOCR) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.DocumentScanner, "OCR")
                        }
                    }

                    // Copy to Clipboard Button
                    IconButton(
                        onClick = {
                            extractedText?.let { text ->
                                clipboardManager.setText(AnnotatedString(text))
                            }
                        },
                        enabled = !extractedText.isNullOrEmpty()
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            "Copy",
                            tint = if (!extractedText.isNullOrEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Tag Button
                    IconButton(
                        onClick = { showTagSheet = true }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Label,
                            "Tags",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // OCR Status/Text Preview
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        when {
                            isProcessingOCR -> {
                                Text(
                                    text = "Extracting text...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            extractedText?.isNotEmpty() == true -> {
                                Text(
                                    text = "Text extracted • Tap copy to clipboard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            extractedText != null -> {
                                Text(
                                    text = "No text found in image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // OCR Result Dialog
    if (showOCRResult && !extractedText.isNullOrEmpty()) {
        AlertDialog(
            onDismissRequest = { showOCRResult = false },
            title = { Text("Extracted Text") },
            text = {
                Text(
                    text = extractedText!!,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(extractedText!!))
                    showOCRResult = false
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOCRResult = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Tag Bottom Sheet
    if (showTagSheet && images.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showTagSheet = false }
        ) {
            FullScreenTagSheet(
                currentImage = images[currentIndex],
                availableTags = tagRepository.getAllAvailableTags(),
                onTagAdded = { tag ->
                    tagRepository.addTagToImage(images[currentIndex].id, tag)
                    // Refresh current image data
                    images = imageRepository.getImagesInFolder(folderPath)
                },
                onTagRemoved = { tagType ->
                    tagRepository.removeTagFromImage(images[currentIndex].id, tagType)
                    // Refresh current image data
                    images = imageRepository.getImagesInFolder(folderPath)
                }
            )
        }
    }
}