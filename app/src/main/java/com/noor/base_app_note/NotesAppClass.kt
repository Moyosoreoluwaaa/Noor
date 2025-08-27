package com.noor.base_app_note

import android.app.Application
import android.os.StrictMode
import com.noor.BuildConfig
import timber.log.Timber

// File: SimpleNotesApplication.kt
// Package: com.noteapp
class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.tag("NotesApplication").d("Application started")

        // Enable strict mode in debug builds
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }
}