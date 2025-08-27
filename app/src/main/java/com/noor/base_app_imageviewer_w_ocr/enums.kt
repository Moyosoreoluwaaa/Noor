package com.noor.base_app_imageviewer_w_ocr

import androidx.compose.ui.graphics.Color

// Enums
enum class SortType {
    DATE_ASCENDING,
    DATE_DESCENDING,
    SIZE_ASCENDING,
    SIZE_DESCENDING
}

enum class FilterType {
    ALL,
    RECENT,
    LARGE_FILES
}

enum class TagType {
    FAVORITE,
    WORK,
    PERSONAL,
    TRAVEL,
    FAMILY,
    SCREENSHOT,
    DOCUMENT,
    MEME,
    IMPORTANT,
    ARCHIVE
}

enum class TagColor(val color: Color) {
    RED(Color(0xFFE57373)),
    PINK(Color(0xFFF06292)),
    PURPLE(Color(0xFFBA68C8)),
    BLUE(Color(0xFF64B5F6)),
    CYAN(Color(0xFF4DD0E1)),
    TEAL(Color(0xFF4DB6AC)),
    GREEN(Color(0xFF81C784)),
    YELLOW(Color(0xFFFFB74D)),
    ORANGE(Color(0xFFFF8A65)),
    BROWN(Color(0xFFA1887F))
}

// Data Classes
data class ImageItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val size: Long,
    val dateModified: Long,
    val folderName: String,
    val folderPath: String,
    val tags: List<ImageTag> = emptyList()
)

data class ImageTag(
    val type: TagType,
    val color: TagColor = getDefaultColorForTag(type),
    val customName: String? = null
) {
    val displayName: String get() = customName ?: type.name.lowercase().replaceFirstChar { it.uppercase() }
}

data class ImageFolder(
    val name: String,
    val path: String,
    val images: List<ImageItem>,
    val coverImage: ImageItem?
)

fun getDefaultColorForTag(tagType: TagType): TagColor = when (tagType) {
    TagType.FAVORITE -> TagColor.RED
    TagType.WORK -> TagColor.BLUE
    TagType.PERSONAL -> TagColor.GREEN
    TagType.TRAVEL -> TagColor.CYAN
    TagType.FAMILY -> TagColor.PINK
    TagType.SCREENSHOT -> TagColor.PURPLE
    TagType.DOCUMENT -> TagColor.BROWN
    TagType.MEME -> TagColor.YELLOW
    TagType.IMPORTANT -> TagColor.ORANGE
    TagType.ARCHIVE -> TagColor.TEAL
}
