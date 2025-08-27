package com.noor.base_app_imageviewer_w_ocr

// Extension Functions
fun List<ImageItem>.sortImages(sortType: SortType): List<ImageItem> {
    return when (sortType) {
        SortType.DATE_ASCENDING -> sortedBy { it.dateModified }
        SortType.DATE_DESCENDING -> sortedByDescending { it.dateModified }
        SortType.SIZE_ASCENDING -> sortedBy { it.size }
        SortType.SIZE_DESCENDING -> sortedByDescending { it.size }
    }
}