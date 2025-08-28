package com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.BottomNavigationBar
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.EmptyStateCard
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.OCRHeaderWithStats
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.PendingImagesSection
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.ProcessingProgressCard
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.ProcessingStepsCard
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.ScanResultCard
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.ScannedImagesPreview
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.viewmodels.OCRViewModel
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.viewmodels.ProcessingStep
import timber.log.Timber

@Composable
fun OCRScreen(
    navController: NavController,
    viewModel: OCRViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(Unit) {
        Timber.d("OCRScreen: LaunchedEffect triggering refreshPendingImages")
        viewModel.refreshPendingImages()
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header with scan stats
            OCRHeaderWithStats(
                pendingCount = uiState.pendingImages.size,
                totalFound = uiState.totalImagesFound,
                onScanClick = {
                    Timber.d("OCRScreen: Scan button clicked")
                    viewModel.scanForNewScreenshots()
                },
                isScanning = uiState.isScanning
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Progress Indicator
            if (uiState.processingStep != ProcessingStep.IDLE) {
                ProcessingStepsCard(
                    currentStep = uiState.processingStep,
                    progress = uiState.scanningProgress,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Scanned Images Preview (max 5)
            if (uiState.scannedImages.isNotEmpty()) {
                ScannedImagesPreview(
                    images = uiState.scannedImages.take(5),
                    totalCount = uiState.scannedImages.size,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Processing Progress (existing)
            if (uiState.isProcessing) {
                ProcessingProgressCard(
                    progress = uiState.processingProgress,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Last Scan Result
            uiState.lastScanResult?.let { scanResult ->
                ScanResultCard(
                    scanResult = scanResult,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Pending Images List
            if (uiState.pendingImages.isNotEmpty()) {
                PendingImagesSection(
                    images = uiState.pendingImages,
                    onProcessSingle = {
                        Timber.d("OCRScreen: Processing single image")
                        viewModel.processSingleImage(it)
                    },
                    onProcessAll = {
                        Timber.d("OCRScreen: Processing all pending images")
                        viewModel.processAllPendingImages()
                    },
                    isProcessing = uiState.isProcessing
                )
            } else if (uiState.processingStep == ProcessingStep.IDLE) {
                EmptyStateCard(
                    onScanClick = {
                        Timber.d("OCRScreen: Empty state scan button clicked")
                        viewModel.scanForNewScreenshots()
                    },
                    isScanning = uiState.isScanning
                )
            }
        }
    }

    // Error Snackbar
    if (uiState.error != null) {
        LaunchedEffect(uiState.error) {
            Timber.e("OCRScreen: Showing snackbar with error: ${uiState.error}")
            viewModel.clearError()
        }
    }

    // Notification Snackbar
    if (uiState.showNotification) {
        LaunchedEffect(uiState.notificationMessage) {
            Timber.d("OCRScreen: Showing notification snackbar: ${uiState.notificationMessage}")
            viewModel.dismissNotification()
        }
    }
}

