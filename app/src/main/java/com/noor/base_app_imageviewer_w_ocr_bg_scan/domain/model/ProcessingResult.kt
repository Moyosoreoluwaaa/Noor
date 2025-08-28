package com.noor.base_app_imageviewer_w_ocr_bg_scan.domain.model

data class ProcessingResult(
    val processed: Int,
    val failed: Int,
    val success: Boolean,
    val message: String
)