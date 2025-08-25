package com.noor.base_app_1.data

// Data Classes
data class ImageItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val size: Long,
    val dateModified: Long,
    val folderName: String,
    val folderPath: String
)

data class ImageFolder(
    val name: String,
    val path: String,
    val images: List<ImageItem>,
    val coverImage: ImageItem?
)