package com.noor.base_app_imageviewer_w_ocr_bg_scan.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.noor.base_app_imageviewer_w_ocr.ImageItem
import com.noor.base_app_imageviewer_w_ocr.OCRProcessor
import com.noor.base_app_imageviewer_w_ocr.TagColor
import com.noor.base_app_imageviewer_w_ocr.TagType
import com.noor.base_app_imageviewer_w_ocr_bg_scan.domain.model.ProcessingResult
import com.noor.base_app_imageviewer_w_ocr_bg_scan.domain.model.ScanResult
import com.noor.base_app_note.data.repository.FixedNoteRepositoryImpl
import com.noor.base_app_note.repository.Note
import com.noor.base_app_note.repository.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 *
 */

// File: OCRRepositoryImpl.kt
// Package: com.noteapp.data.repository

class OCRRepositoryImpl(
    private val context: Context,
    private val noteRepository: FixedNoteRepositoryImpl,
    private val ocrProcessor: OCRProcessor
) {
    companion object {
        private const val TAG = "OCRRepository"
        private const val PREFS_NAME = "ocr_prefs"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
        private const val KEY_PROCESSED_IMAGES = "processed_images"
        private const val KEY_PENDING_COUNT = "pending_count"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _pendingImages = MutableStateFlow<List<ImageItem>>(emptyList())
    val pendingImages: StateFlow<List<ImageItem>> = _pendingImages.asStateFlow()

    private val _processedCount = MutableStateFlow(0)
    val processedCount: StateFlow<Int> = _processedCount.asStateFlow()

    suspend fun scanForNewScreenshots(): ScanResult = withContext(Dispatchers.IO) {
        try {
            Timber.Forest.tag(TAG).d("Starting screenshot scan using MediaStore")

            if (!hasMediaPermission()) {
                Timber.Forest.tag(TAG).w("Media permission not granted")
                return@withContext ScanResult(
                    newImagesFound = 0,
                    totalPending = 0,
                    success = false,
                    message = "Media permission required to scan screenshots"
                )
            }

            val processedImages = getProcessedImages()
            val lastScanTime = sharedPrefs.getLong(KEY_LAST_SCAN_TIME, 0L)
            val newImages = mutableListOf<ImageItem>()

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATA
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

            // Query ALL images first (like your working ImageRepository)
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, // No selection filter
                null, // No selection args
                sortOrder
            )?.use { cursor ->
                Timber.Forest.d("Total images in MediaStore: ${cursor.count}")
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateModified = cursor.getLong(dateColumn) // This is in seconds
                    val data = cursor.getString(dataColumn)

                    val file = File(data)
                    val folderName = file.parent?.let { File(it).name } ?: ""

                    // Filter in-memory instead of at MediaStore level
                    val isScreenshot = folderName.contains("screenshot", ignoreCase = true) ||
                            data.contains("screenshot", ignoreCase = true) ||
                            name.contains("screenshot", ignoreCase = true)

                    val isNewEnough = dateModified * 1000 > lastScanTime

                    if (isScreenshot && isNewEnough && !processedImages.contains(data)) {
                        Timber.Forest.tag(TAG).d("Found new screenshot: $name from $data")

                        val imageItem = ImageItem(
                            id = id,
                            uri = Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            ).toString(),
                            displayName = name,
                            size = size,
                            dateModified = dateModified * 1000, // Convert to milliseconds
                            folderName = "Screenshots",
                            folderPath = File(data).parent ?: "Screenshots"
                        )
                        newImages.add(imageItem)
                    } else {
                        Timber.Forest.d("Skipping image: $name (not a new screenshot or already processed)")
                    }
                }
            }

            // Update pending images list
            val currentPending = _pendingImages.value.toMutableList()
            newImages.forEach { newImage ->
                if (!currentPending.any { it.uri == newImage.uri }) {
                    currentPending.add(newImage)
                }
            }
            _pendingImages.value = currentPending

            // Update last scan time
            sharedPrefs.edit { putLong(KEY_LAST_SCAN_TIME, System.currentTimeMillis()) }

            val totalPending = currentPending.size
            sharedPrefs.edit { putInt(KEY_PENDING_COUNT, totalPending) }

            Timber.Forest.tag(TAG)
                .d("Scan complete. New: ${newImages.size}, Total pending: $totalPending")

            return@withContext ScanResult(
                newImagesFound = newImages.size,
                totalPending = totalPending,
                success = true,
                message = when {
                    newImages.size >= 5 -> "Found ${newImages.size} new screenshots!"
                    newImages.size > 0 -> "Found ${newImages.size} new screenshot${if (newImages.size > 1) "s" else ""}"
                    else -> "You're doing well! No new screenshots today."
                }
            )

        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "Error during screenshot scan")
            return@withContext ScanResult(
                newImagesFound = 0,
                totalPending = 0,
                success = false,
                message = "Scan failed: ${e.message}"
            )
        }
    }

    suspend fun processImage(imageItem: ImageItem): Result<Note> = withContext(Dispatchers.IO) {
        try {
            Timber.Forest.tag(TAG).d("Processing image: ${imageItem.displayName}")

            val extractedText = ocrProcessor.extractText(imageItem.uri)

            if (extractedText.isBlank()) {
                Timber.Forest.tag(TAG).w("No text extracted from image: ${imageItem.displayName}")
                return@withContext Result.failure(Exception("No text found in image"))
            }

            // Generate title from date + first line of extracted text
            val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(Date(imageItem.dateModified))

            val firstLine = extractedText.lines()
                .firstOrNull { it.trim().isNotEmpty() }
                ?.trim()
                ?.take(30) // Limit to 30 characters
                ?.let { if (it.length == 30) "$it..." else it }

            val noteTitle = if (firstLine != null) {
                "OCR $timestamp - $firstLine"
            } else {
                "OCR $timestamp"
            }

            // Create note using existing repository
            val createResult = noteRepository.createNote(noteTitle)

            if (createResult.isFailure) {
                return@withContext Result.failure(
                    createResult.exceptionOrNull() ?: Exception("Failed to create note")
                )
            }

            val createdNote = createResult.getOrThrow()

            // Update note content with OCR data and tags
            val screenshotTag = Tag(
                type = TagType.SCREENSHOT,
                color = TagColor.BLUE
            )

            val markdownContent = buildString {
                appendLine("# $noteTitle")
                appendLine()
                appendLine("**Source:** ${imageItem.displayName}")
                appendLine(
                    "**Date:** ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date(imageItem.dateModified))
                    }"
                )
                appendLine()
                appendLine("## Extracted Text")
                appendLine()
                appendLine(extractedText.trim())
                appendLine()
                appendLine("---")
                appendLine(
                    "*Processed with OCR on ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                    }*"
                )
            }

            val updatedNote = createdNote.copy(
                content = markdownContent,
                tags = listOf(screenshotTag)
            ).updateWordCount()

            // Save the updated note
            val saveResult = noteRepository.saveNote(updatedNote)

            if (saveResult.isSuccess) {
                // Mark image as processed
                markImageAsProcessed(imageItem)

                // Remove from pending list
                val updatedPending = _pendingImages.value.filter { it.uri != imageItem.uri }
                _pendingImages.value = updatedPending

                // Update processed count
                _processedCount.value = _processedCount.value + 1

                Timber.Forest.tag(TAG)
                    .d("Successfully processed image and created note: ${updatedNote.title}")
                Result.success(saveResult.getOrThrow())
            } else {
                // Clean up created note if save failed
                noteRepository.deleteNote(createdNote)
                Result.failure(
                    saveResult.exceptionOrNull() ?: Exception("Failed to save note content")
                )
            }

        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "Error processing image: ${imageItem.displayName}")
            Result.failure(e)
        }
    }

    suspend fun processPendingImages(): ProcessingResult = withContext(Dispatchers.IO) {
        val pending = _pendingImages.value
        if (pending.isEmpty()) {
            return@withContext ProcessingResult(
                processed = 0,
                failed = 0,
                success = true,
                message = "No images to process"
            )
        }

        var processedCount = 0
        var failedCount = 0

        pending.forEach { imageItem ->
            val result = processImage(imageItem)
            if (result.isSuccess) {
                processedCount++
            } else {
                failedCount++
                Timber.Forest.tag(TAG).w("Failed to process: ${imageItem.displayName}")
            }
        }

        ProcessingResult(
            processed = processedCount,
            failed = failedCount,
            success = failedCount == 0,
            message = "Processed $processedCount images" +
                    if (failedCount > 0) ", $failedCount failed" else ""
        )
    }

    fun getPendingCount(): Int = _pendingImages.value.size

    fun getProcessedCount(): Int = _processedCount.value

    private fun getProcessedImages(): Set<String> {
        return try {
            val processedJson = sharedPrefs.getString(KEY_PROCESSED_IMAGES, null)
            if (processedJson != null) {
                val type = object : TypeToken<Set<String>>() {}.type
                gson.fromJson(processedJson, type)
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "Error loading processed images")
            emptySet()
        }
    }

    private fun markImageAsProcessed(imageItem: ImageItem) {
        try {
            val processedImages = getProcessedImages().toMutableSet()
            // Use the actual file path from MediaStore data
            val uri = Uri.parse(imageItem.uri)
            val imagePath = if (uri.scheme == "content") {
                // For content URI, use the URI string itself as identifier
                imageItem.uri
            } else {
                // For file URI, use the path
                File(uri.path ?: "").absolutePath
            }
            processedImages.add(imagePath)

            val processedJson = gson.toJson(processedImages)
            sharedPrefs.edit { putString(KEY_PROCESSED_IMAGES, processedJson) }

            Timber.Forest.tag(TAG).d("Marked as processed: ${imageItem.displayName}")
        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "Error marking image as processed")
        }
    }

    fun getAvailableScreenshotFolders(): List<String> {
        val folders = mutableSetOf<String>()

        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("%Screenshot%")

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val data = cursor.getString(dataColumn)
                    val folder = File(data).parent
                    if (folder != null) {
                        folders.add(folder)
                    }
                }
            }

            Timber.Forest.tag(TAG).d("Found screenshot folders: ${folders.joinToString()}")
            return folders.toList()

        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "Error getting screenshot folders")
            return emptyList()
        }
    }

    // Helper function to check if we have media permissions
    fun hasMediaPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun getDefaultColorForTag(type: TagType): TagColor {
        return when (type) {
            TagType.SCREENSHOT -> TagColor.BLUE
            TagType.DOCUMENT -> TagColor.GREEN
            TagType.IMPORTANT -> TagColor.RED
            TagType.WORK -> TagColor.PURPLE
            TagType.PERSONAL -> TagColor.TEAL
            else -> TagColor.BLUE
        }
    }
}

/**
 *
 */