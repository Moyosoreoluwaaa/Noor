package com.noor.base_app_imageviewer_w_ocr_bg_scan.worker// File: ScreenshotScanWorker.kt
// Package: com.noteapp.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.util.Calendar
import androidx.annotation.RequiresPermission
import androidx.compose.ui.util.trace
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.noor.R
import com.noor.base_app_imageviewer_w_ocr.OCRProcessor
import com.noor.base_app_imageviewer_w_ocr_bg_scan.data.repository.OCRRepositoryImpl
import com.noor.base_app_imageviewer_w_ocr_bg_scan.domain.model.ScanResult
import com.noor.base_app_note.data.repository.FixedNoteRepositoryImpl
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ScreenshotScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ScreenshotScanWorker"
        const val WORK_NAME = "screenshot_scan_work"
        private const val NOTIFICATION_CHANNEL_ID = "ocr_scan_channel"
        private const val NOTIFICATION_ID = 1001
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result = trace("ScreenshotScanWorker") {
        try {
            Timber.tag(TAG).d("Starting background screenshot scan")

            // Create repository instances (manual DI for now)
            val noteRepository = FixedNoteRepositoryImpl(applicationContext)
            val ocrProcessor = OCRProcessor(applicationContext)
            val ocrRepository = OCRRepositoryImpl(applicationContext, noteRepository, ocrProcessor)

            val scanResult = ocrRepository.scanForNewScreenshots()

            Timber.tag(TAG).d("Background scan completed: ${scanResult.message}")

            // Show notification if new images found or daily "doing well" message
            showNotification(scanResult)

            if (scanResult.success) {
                Result.success()
            } else {
                Timber.tag(TAG).w("Scan completed with issues: ${scanResult.message}")
                Result.retry()
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Background scan failed")
            Result.failure()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(scanResult: ScanResult) {
        try {
            createNotificationChannel()

            val notificationTitle = when {
                scanResult.newImagesFound >= 5 -> "📸 Many Screenshots Found!"
                scanResult.newImagesFound > 0 -> "📸 New Screenshots Found"
                else -> "✨ You're Doing Well!"
            }

            val notificationText = when {
                scanResult.newImagesFound >= 5 ->
                    "${scanResult.newImagesFound} new screenshots ready for OCR processing"
                scanResult.newImagesFound > 0 ->
                    "${scanResult.newImagesFound} new screenshot${if(scanResult.newImagesFound > 1) "s" else ""} found"
                else ->
                    "No new screenshots today. Keep up the good work!"
            }

            // Create intent to open the app
            val intent = applicationContext.packageManager
                .getLaunchIntentForPackage(applicationContext.packageName)
                ?.apply {
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigate_to_ocr", true)
                }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                .setPriority(if (scanResult.newImagesFound >= 5) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = NotificationManagerCompat.from(applicationContext)
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, notification)
                Timber.tag(TAG).d("Notification sent: $notificationTitle")
            } else {
                Timber.tag(TAG).w("Notification permission not granted")
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error showing notification")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "OCR Screenshot Scanning",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for new screenshot detection and OCR processing"
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
        }

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}

// File: WorkManagerSetup.kt
// Package: com.noteapp.worker

object WorkManagerSetup {
    private const val TAG = "WorkManagerSetup"

    fun scheduleScreenshotScanning(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .build()

            // Schedule twice daily - morning and evening
            val morningWork = PeriodicWorkRequestBuilder<ScreenshotScanWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayToNextMorning(), TimeUnit.MILLISECONDS)
                .addTag("morning_scan")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    ScreenshotScanWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    morningWork
                )

            Timber.tag(TAG).d("Screenshot scanning work scheduled")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error scheduling screenshot scanning work")
        }
    }

    fun cancelScreenshotScanning(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(ScreenshotScanWorker.WORK_NAME)
        Timber.tag(TAG).d("Screenshot scanning work cancelled")
    }

    fun triggerImmediateScan(context: Context) {
        try {
            val immediateWork = OneTimeWorkRequestBuilder<ScreenshotScanWorker>()
                .addTag("immediate_scan")
                .build()

            WorkManager.getInstance(context).enqueue(immediateWork)
            Timber.tag(TAG).d("Immediate scan triggered")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error triggering immediate scan")
        }
    }

    private fun calculateDelayToNextMorning(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8) // 8 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If it's already past 8 AM today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()
        Timber.tag(TAG).d("Next morning scan scheduled in ${delay / (1000 * 60)} minutes")
        return delay
    }
}