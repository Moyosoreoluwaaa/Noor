package com.noor.base_app_imageviewer_w_ocr_bg_scan.domain.model

data class ScanResult(
    val newImagesFound: Int,
    val totalPending: Int,
    val success: Boolean,
    val message: String
)