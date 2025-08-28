package com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noor.base_app_imageviewer_w_ocr.ImageItem
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PendingImagesSection(
    images: ImmutableList<ImageItem>,
    onProcessSingle: (ImageItem) -> Unit,
    onProcessAll: () -> Unit,
    isProcessing: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pending Images (${images.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (images.size > 1) {
                Button(
                    onClick = onProcessAll,
                    enabled = !isProcessing,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Process All",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = images,
                key = { it.uri }
            ) { imageItem ->
                PendingImageCard(
                    imageItem = imageItem,
                    onProcess = { onProcessSingle(imageItem) },
                    isProcessing = isProcessing
                )
            }
        }
    }
}