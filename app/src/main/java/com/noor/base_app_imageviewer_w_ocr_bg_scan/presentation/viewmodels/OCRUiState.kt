package com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.viewmodels

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.base_app_imageviewer_w_ocr.ImageItem
import com.noor.base_app_imageviewer_w_ocr_bg_scan.data.repository.OCRRepositoryImpl
import com.noor.base_app_imageviewer_w_ocr_bg_scan.domain.model.ScanResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 *
 */

// File: OCRViewModel.kt
// Package: com.noteapp.presentation.viewmodel

@Stable
data class OCRUiState(
    val pendingImages: ImmutableList<ImageItem> = persistentListOf(),
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val lastScanResult: ScanResult? = null,
    val processingProgress: ProcessingProgress = ProcessingProgress(),
    val error: String? = null,
    val showNotification: Boolean = false,
    val notificationMessage: String = "",
    val scannedImages: List<ImageItem> = emptyList(), // For preview display
    val totalImagesFound: Int = 0,
    val scanningProgress: Float = 0f, // 0.0 to 1.0
    val processingStep: ProcessingStep = ProcessingStep.IDLE
)

enum class ProcessingStep {
    IDLE,
    SCANNING,
    PROCESSING_OCR,
    CREATING_NOTES,
    COMPLETED
}


@Immutable
data class ProcessingProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentImageName: String = ""
) {
    val percentage: Float
        get() = if (total > 0) current.toFloat() / total.toFloat() else 0f
}

class OCRViewModel(
    private val ocrRepository: OCRRepositoryImpl
) : ViewModel() {

    companion object {
        private const val TAG = "OCRViewModel"
    }

    private val _uiState = MutableStateFlow(OCRUiState())
    val uiState: StateFlow<OCRUiState> = _uiState.asStateFlow()

    init {
        Timber.tag(TAG).d("ViewModel initialized")
        loadPendingImages()
        observeRepositoryUpdates()
    }

    fun scanForNewScreenshots() {
        Timber.d("scanAndProcessScreenshots() called - START")
        viewModelScope.launch {
            Timber.d("Inside viewModelScope.launch")
            try {
                Timber.tag(TAG).d("Starting manual screenshot scan")

                _uiState.update { it.copy(isScanning = true, error = null) }

                val scanResult = ocrRepository.scanForNewScreenshots()

                _uiState.update {
                    it.copy(
                        isScanning = false,
                        lastScanResult = scanResult,
                        showNotification = scanResult.newImagesFound >= 5,
                        notificationMessage = scanResult.message
                    )
                }

                Timber.tag(TAG).d("Scan completed: ${scanResult.message}")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error during manual scan")
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error = "Scan failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun processAllPendingImages() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Starting to process all pending images")

                val pendingImages = _uiState.value.pendingImages
                if (pendingImages.isEmpty()) {
                    _uiState.update { it.copy(error = "No images to process") }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isProcessing = true,
                        error = null,
                        processingProgress = ProcessingProgress(total = pendingImages.size)
                    )
                }

                var processedCount = 0
                var failedCount = 0

                pendingImages.forEachIndexed { index, imageItem ->
                    try {
                        _uiState.update {
                            it.copy(
                                processingProgress = it.processingProgress.copy(
                                    current = index + 1,
                                    currentImageName = imageItem.displayName
                                )
                            )
                        }

                        val result = ocrRepository.processImage(imageItem)
                        if (result.isSuccess) {
                            processedCount++
                            Timber.tag(TAG).d("Successfully processed: ${imageItem.displayName}")
                        } else {
                            failedCount++
                            Timber.tag(TAG).w("Failed to process: ${imageItem.displayName} - ${result.exceptionOrNull()?.message}")
                        }

                        // Small delay to show progress
                        delay(300)

                    } catch (e: Exception) {
                        failedCount++
                        Timber.tag(TAG).e(e, "Error processing: ${imageItem.displayName}")
                    }
                }

                val message = when {
                    failedCount == 0 -> "Successfully created $processedCount notes from screenshots!"
                    processedCount == 0 -> "Failed to process all images. Please try again."
                    else -> "Created $processedCount notes, $failedCount failed"
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingProgress = ProcessingProgress(),
                        notificationMessage = message,
                        showNotification = true
                    )
                }

                Timber.tag(TAG).d("Processing completed: $message")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error during batch processing")
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingProgress = ProcessingProgress(),
                        error = "Processing failed. Please try again."
                    )
                }
            }
        }
    }

    fun processSingleImage(imageItem: ImageItem) {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Processing single image: ${imageItem.displayName}")

                _uiState.update { it.copy(isProcessing = true, error = null) }

                val result = ocrRepository.processImage(imageItem)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            notificationMessage = "Created note from ${imageItem.displayName}",
                            showNotification = true
                        )
                    }
                    Timber.tag(TAG).d("Successfully processed single image")
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = "Failed to process ${imageItem.displayName}: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        )
                    }
                }

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error processing single image")
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Processing failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissNotification() {
        _uiState.update {
            it.copy(
                showNotification = false,
                notificationMessage = ""
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refreshPendingImages() {
        loadPendingImages()
    }

    private fun loadPendingImages() {
        viewModelScope.launch {
            try {
                Timber.tag(TAG).d("Loading pending images")
                ocrRepository.pendingImages.collect { images ->
                    _uiState.update {
                        it.copy(pendingImages = images.toImmutableList())
                    }
                    Timber.tag(TAG).d("Updated pending images: ${images.size}")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading pending images")
                _uiState.update {
                    it.copy(error = "Failed to load pending images: ${e.message}")
                }
            }
        }
    }

    private fun observeRepositoryUpdates() {
        viewModelScope.launch {
            // Observe processed count updates
            ocrRepository.processedCount.collect { count ->
                Timber.tag(TAG).d("Processed count updated: $count")
                // You can update UI state here if needed for statistics
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.tag(TAG).d("ViewModel cleared")
    }
}

/**
 *
 */
//@Stable
//data class OCRUiState(
//    val pendingImages: ImmutableList<ImageItem> = persistentListOf(),
//    val isScanning: Boolean = false,
//    val isProcessing: Boolean = false,
//    val lastScanResult: ScanResult? = null,
//    val processingProgress: ProcessingProgress = ProcessingProgress(),
//    val error: String? = null,
//    val showNotification: Boolean = false,
//    val notificationMessage: String = ""
//)
//
//@Immutable
//data class ProcessingProgress(
//    val current: Int = 0,
//    val total: Int = 0,
//    val currentImageName: String = ""
//) {
//    val percentage: Float
//        get() = if (total > 0) current.toFloat() / total.toFloat() else 0f
//}
//
//class OCRViewModel(
//    private val ocrRepository: OCRRepositoryImpl
//) : ViewModel() {
//
//    companion object {
//        private const val TAG = "OCRViewModel"
//    }
//
//    private val _uiState = MutableStateFlow(OCRUiState())
//    val uiState: StateFlow<OCRUiState> = _uiState.asStateFlow()
//
//    init {
//        Timber.tag(TAG).d("ViewModel initialized")
//        loadPendingImages()
//        observeRepositoryUpdates()
//    }
//
//    fun scanForNewScreenshots() {
//        viewModelScope.launch {
//            try {
//                Timber.tag(TAG).d("Starting manual screenshot scan")
//                _uiState.update { it.copy(isScanning = true, error = null) }
//                val scanResult = ocrRepository.scanForNewScreenshots()
//                _uiState.update {
//                    it.copy(
//                        isScanning = false,
//                        lastScanResult = scanResult,
//                        showNotification = scanResult.newImagesFound >= 5,
//                        notificationMessage = scanResult.message
//                    )
//                }
//                Timber.tag(TAG).d("Scan completed: ${scanResult.message}")
//            } catch (e: Exception) {
//                Timber.tag(TAG).e(e, "Error during manual scan")
//                _uiState.update {
//                    it.copy(
//                        isScanning = false,
//                        error = "Scan failed: ${e.message}"
//                    )
//                }
//            }
//        }
//    }
//
//    fun processAllPendingImages() {
//        viewModelScope.launch {
//            try {
//                Timber.tag(TAG).d("Starting to process all pending images")
//                val pendingImages = _uiState.value.pendingImages
//                if (pendingImages.isEmpty()) {
//                    Timber.tag(TAG).d("No images to process, updating UI with error message.")
//                    _uiState.update { it.copy(error = "No images to process") }
//                    return@launch
//                }
//                _uiState.update {
//                    it.copy(
//                        isProcessing = true,
//                        error = null,
//                        processingProgress = ProcessingProgress(total = pendingImages.size)
//                    )
//                }
//                var processedCount = 0
//                var failedCount = 0
//                pendingImages.forEachIndexed { index, imageItem ->
//                    try {
//                        _uiState.update {
//                            it.copy(
//                                processingProgress = it.processingProgress.copy(
//                                    current = index,
//                                    currentImageName = imageItem.displayName
//                                )
//                            )
//                        }
//                        val result = ocrRepository.processImage(imageItem)
//                        if (result.isSuccess) {
//                            processedCount++
//                            Timber.tag(TAG).d("Successfully processed: ${imageItem.displayName}")
//                        } else {
//                            failedCount++
//                            Timber.tag(TAG).w("Failed to process: ${imageItem.displayName}")
//                        }
//                        delay(100)
//                    } catch (e: Exception) {
//                        failedCount++
//                        Timber.tag(TAG).e(e, "Error processing: ${imageItem.displayName}")
//                    }
//                }
//                val message = "Processing complete! $processedCount processed" +
//                        if (failedCount > 0) ", $failedCount failed" else ""
//                _uiState.update {
//                    it.copy(
//                        isProcessing = false,
//                        processingProgress = ProcessingProgress(),
//                        notificationMessage = message,
//                        showNotification = true
//                    )
//                }
//                Timber.tag(TAG).d("Processing completed: $message")
//            } catch (e: Exception) {
//                Timber.tag(TAG).e(e, "Error during batch processing")
//                _uiState.update {
//                    it.copy(
//                        isProcessing = false,
//                        processingProgress = ProcessingProgress(),
//                        error = "Processing failed: ${e.message}"
//                    )
//                }
//            }
//        }
//    }
//
//    fun processSingleImage(imageItem: ImageItem) {
//        viewModelScope.launch {
//            try {
//                Timber.tag(TAG).d("Processing single image: ${imageItem.displayName}")
//                _uiState.update { it.copy(isProcessing = true, error = null) }
//                val result = ocrRepository.processImage(imageItem)
//                if (result.isSuccess) {
//                    _uiState.update {
//                        it.copy(
//                            isProcessing = false,
//                            notificationMessage = "Successfully processed ${imageItem.displayName}",
//                            showNotification = true
//                        )
//                    }
//                    Timber.tag(TAG).d("Successfully processed single image")
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            isProcessing = false,
//                            error = "Failed to process ${imageItem.displayName}"
//                        )
//                    }
//                    Timber.tag(TAG).w("Failed to process single image")
//                }
//            } catch (e: Exception) {
//                Timber.tag(TAG).e(e, "Error processing single image")
//                _uiState.update {
//                    it.copy(
//                        isProcessing = false,
//                        error = "Processing failed: ${e.message}"
//                    )
//                }
//            }
//        }
//    }
//
//    fun dismissNotification() {
//        _uiState.update {
//            it.copy(
//                showNotification = false,
//                notificationMessage = ""
//            )
//        }
//    }
//
//    fun clearError() {
//        _uiState.update { it.copy(error = null) }
//    }
//
//    fun refreshPendingImages() {
//        Timber.tag(TAG).d("refreshPendingImages called")
//        loadPendingImages()
//    }
//
//    private fun loadPendingImages() {
//        viewModelScope.launch {
//            try {
//                Timber.tag(TAG).d("Loading pending images")
//                ocrRepository.pendingImages.collect { images ->
//                    _uiState.update {
//                        it.copy(pendingImages = images.toImmutableList())
//                    }
//                    Timber.tag(TAG).d("Updated pending images: ${images.size}")
//                }
//            } catch (e: Exception) {
//                Timber.tag(TAG).e(e, "Error loading pending images")
//                _uiState.update {
//                    it.copy(error = "Failed to load pending images: ${e.message}")
//                }
//            }
//        }
//    }
//
//    private fun observeRepositoryUpdates() {
//        viewModelScope.launch {
//            ocrRepository.processedCount.collect { count ->
//                Timber.tag(TAG).d("Processed count updated: $count")
//            }
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        Timber.tag(TAG).d("ViewModel cleared")
//    }
//}
