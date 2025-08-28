package com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.noor.R
import com.noor.base_app_imageviewer_w_ocr.ImageItem

@Composable
fun ImagePreviewItem(
    image: ImageItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(80.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.ic_launcher_background),
            placeholder = painterResource(R.drawable.ic_launcher_background)
        )
    }
}