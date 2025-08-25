package com.noor.base_app_1.data
// FullScreenImageScreen.kt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageScreen(
    initialIndex: Int,
    folderPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val imageRepository = remember { ImageRepository(context) }
    
    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var isTopBarVisible by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    LaunchedEffect(folderPath) {
        images = imageRepository.getImagesInFolder(folderPath)
    }
    
    LaunchedEffect(Unit) {
        delay(3000) // Auto-hide top bar after 3 seconds
        isTopBarVisible = false
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
                                        // Swipe right - previous image
                                        if (currentIndex > 0) {
                                            currentIndex--
                                            scale = 1f
                                            offset = Offset.Zero
                                        }
                                    } else if (offset.x < -100) {
                                        // Swipe left - next image
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
                        Text(text = images[currentIndex].displayName)
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
    }
}