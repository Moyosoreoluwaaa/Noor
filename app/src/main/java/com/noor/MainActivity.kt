//package com.noor
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.navigation.compose.rememberNavController
//import com.noor.base_app_imageviewer_w_ocr.OCRProcessor
//import com.noor.base_app_imageviewer_w_ocr_bg_scan.data.repository.OCRRepositoryImpl
//import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.viewmodels.OCRViewModel
//import com.noor.base_app_imageviewer_w_ocr_bg_scan.worker.WorkManagerSetup
//import com.noor.base_app_note.FixedEditorViewModel
//import com.noor.base_app_note.NotesViewModel
//import com.noor.base_app_note.PermissionScreen
//import com.noor.base_app_note.Routes
//import com.noor.base_app_note.data.permissions.PermissionHandler
//import com.noor.base_app_note.data.repository.FixedNoteRepositoryImpl
//import com.noor.base_app_note.presentation.navigation.NotesNavigation
//import com.noor.ui.theme.NoorTheme
//import timber.log.Timber
//
//class MainActivity : ComponentActivity() {
//
//    private var noteRepository: FixedNoteRepositoryImpl? = null
//    private var notesViewModel: NotesViewModel? = null
//    private var editorViewModel: FixedEditorViewModel? = null
//    private var ocrRepository: OCRRepositoryImpl? = null
//    private var ocrViewModel: OCRViewModel? = null
//    private lateinit var permissionHandler: PermissionHandler
//
//    companion object {
//        private const val TAG = "MainActivity"
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        intent = intent
//        permissionHandler = PermissionHandler(this)
//
//        setContent {
//            NoorTheme {
//                val navController = rememberNavController()
//
//                LaunchedEffect(Unit) {
//                    if (intent.getBooleanExtra("navigate_to_ocr", false)) {
//                        navController.navigate(Routes.OCR_SCREEN)
//                        Timber.tag(TAG).d("Navigating to OCR screen from onCreate LaunchedEffect")
//                    }
//                }
//
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    var hasPermissions by remember { mutableStateOf(permissionHandler.hasStoragePermissions()) }
//                    var dependenciesReady by remember { mutableStateOf(false) }
//
//                    LaunchedEffect(hasPermissions) {
//                        if (hasPermissions && !dependenciesReady) {
//                            setupDependencies()
//                            dependenciesReady = true
//                            initializeBackgroundWork()
//                        }
//                    }
//
//                    when {
//                        !hasPermissions -> {
//                            PermissionScreen(
//                                onPermissionGranted = {
//                                    hasPermissions = true
//                                    showStorageLocationToast()
//                                },
//                                onPermissionDenied = {
//                                    Toast.makeText(
//                                        this@MainActivity,
//                                        "Storage permission is required for full functionality",
//                                        Toast.LENGTH_LONG
//                                    ).show()
//                                    hasPermissions = true
//                                }
//                            )
//                        }
//
//                        !dependenciesReady -> {
//                            Box(
//                                modifier = Modifier.fillMaxSize(),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Column(
//                                    horizontalAlignment = Alignment.CenterHorizontally
//                                ) {
//                                    CircularProgressIndicator()
//                                    Spacer(modifier = Modifier.height(16.dp))
//                                    Text(
//                                        text = "Setting up your notes...",
//                                        style = MaterialTheme.typography.bodyLarge
//                                    )
//                                }
//                            }
//                        }
//
//                        else -> {
//                            NotesNavigation(
//                                navController = navController,
//                                notesViewModel = notesViewModel!!,
//                                editorViewModel = editorViewModel!!,
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        setIntent(intent)
//        Timber.tag(TAG).d("Received new intent to navigate to OCR screen")
//    }
//
//    private fun setupDependencies() {
//        try {
//            Timber.tag(TAG).d("Setting up dependencies...")
//            noteRepository = FixedNoteRepositoryImpl(context = applicationContext)
//
//            val ocrProcessor = OCRProcessor(applicationContext)
//            ocrRepository = OCRRepositoryImpl(applicationContext, noteRepository!!, ocrProcessor)
//
//            // Now pass ocrRepository to NotesViewModel
//            notesViewModel =
//                NotesViewModel(repository = noteRepository!!, ocrRepository = ocrRepository!!)
//            editorViewModel = FixedEditorViewModel(repository = noteRepository!!)
//            ocrViewModel = OCRViewModel(ocrRepository!!)
//
//            Timber.tag(TAG).d("Dependencies initialized successfully")
//            Timber.tag(TAG).d("Notes will be saved to: ${noteRepository!!.getStoragePath()}")
//        } catch (e: Exception) {
//            Timber.tag(TAG).e(e, "Error setting up dependencies")
//            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun initializeBackgroundWork() {
//        try {
//            WorkManagerSetup.scheduleScreenshotScanning(this)
//            Timber.tag(TAG).d("Background work initialized")
//        } catch (e: Exception) {
//            Timber.tag(TAG).e(e, "Failed to initialize background work")
//        }
//    }
//
//    private fun showStorageLocationToast() {
//        Toast.makeText(
//            this,
//            "Notes will be saved to Documents/MarkdownNotes",
//            Toast.LENGTH_LONG
//        ).show()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        Timber.tag(TAG).d("MainActivity destroyed")
//    }
//}
//
