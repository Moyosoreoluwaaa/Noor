package com.noor

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noor.base_app_imageviewer_w_ocr.AlbumScreen
import com.noor.base_app_imageviewer_w_ocr.FullScreenImageScreen
import com.noor.base_app_note.FixedEditorViewModel
import com.noor.base_app_note.NotesNavigation
import com.noor.base_app_note.NotesViewModel
import com.noor.base_app_note.PermissionScreen
import com.noor.base_app_note.repository.FixedNoteRepositoryImpl
import com.noor.base_app_note.repository.PermissionHandler
import com.noor.ui.theme.NoorTheme
import java.net.URLDecoder
import java.net.URLEncoder


// File: FixedMainActivity.kt
// Package: com.noteapp
class MainActivity : ComponentActivity() {

    private var noteRepository: FixedNoteRepositoryImpl? = null
    private var notesViewModel: NotesViewModel? = null
    private var editorViewModel: FixedEditorViewModel? = null
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission handler
        permissionHandler = PermissionHandler(this)

        setContent {
            NoorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasPermissions by remember { mutableStateOf(permissionHandler.hasStoragePermissions()) }
                    var dependenciesReady by remember { mutableStateOf(false) }

                    // Initialize dependencies when permissions are granted
                    LaunchedEffect(hasPermissions) {
                        if (hasPermissions && !dependenciesReady) {
                            setupDependencies()
                            dependenciesReady = true
                        }
                    }

                    when {
                        !hasPermissions -> {
                            PermissionScreen(
                                onPermissionGranted = {
                                    hasPermissions = true
                                    showStorageLocationToast()
                                },
                                onPermissionDenied = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Storage permission is required for full functionality",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    hasPermissions = true // Allow limited functionality
                                }
                            )
                        }

                        !dependenciesReady -> {
                            // Show loading while dependencies are being set up
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Setting up your notes...",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }

                        else -> {
                            // Both permissions granted and dependencies ready
                            val navController = rememberNavController()

                            NotesNavigation(
                                navController = navController,
                                notesViewModel = notesViewModel!!,
                                editorViewModel = editorViewModel!!
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupDependencies() {
        try {
            Log.d("MainActivity", "Setting up dependencies...")

            // Create repository with fixed implementation
            noteRepository = FixedNoteRepositoryImpl(context = applicationContext)

            // Create ViewModels with manual injection
            notesViewModel = NotesViewModel(repository = noteRepository!!)
            editorViewModel = FixedEditorViewModel(repository = noteRepository!!)

            Log.d("MainActivity", "Dependencies initialized successfully")
            Log.d("MainActivity", "Notes will be saved to: ${noteRepository!!.getStoragePath()}")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up dependencies", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showStorageLocationToast() {
        Toast.makeText(
            this,
            "Notes will be saved to Documents/MarkdownNotes",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up ViewModels manually since we're not using Hilt
//        notesViewModel?.onCleared()
//        editorViewModel?.onCleared()
    }
}


//class MainActivity : ComponentActivity() {
//
//    // Manual DI - Create dependencies manually
//    private lateinit var noteRepository: NoteRepositoryImpl
//    private lateinit var notesViewModel: NotesViewModel
//    private lateinit var editorViewModel: EditorViewModel
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Initialize dependencies manually
//        setupDependencies()
//
//        setContent {
//            NoorTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    val navController = rememberNavController()
//
//                    NotesNavigation(
//                        navController = navController,
//                        notesViewModel = notesViewModel,
//                        editorViewModel = editorViewModel
//                    )
//                }
//            }
//        }
//    }
//
//    private fun setupDependencies() {
//        // Create repository
//        noteRepository = NoteRepositoryImpl(context = applicationContext)
//
//        // Create ViewModels with manual injection
//        notesViewModel = NotesViewModel(repository = noteRepository)
//        editorViewModel = EditorViewModel(repository = noteRepository)
//    }
//
////    override fun onDestroy() {
////        super.onDestroy()
////        // Clean up ViewModels manually since we're not using Hilt
////        if (::notesViewModel.isInitialized) {
////            notesViewModel.onCleared()
////        }
////        if (::editorViewModel.isInitialized) {
////            editorViewModel.onCleared()
////        }
////    }
//
//}


//////////////////
//@AndroidEntryPoint
//class MainActivity : ComponentActivity() {
//    private val requestPermission = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        if (!isGranted) {
//            finish()
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Request permission using Android 14 bottom sheet
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            requestPermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
//        } else {
//            requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
//        }
//
//        setContent {
//            NoorTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    NoorApp()
//                }
//            }
//        }
//    }
//}

@Composable
fun NoorApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "albums"
    ) {
        composable("albums") {
            AlbumScreen(
                onImageClick = { imageIndex, folderPath ->
                    val encodedPath = URLEncoder.encode(folderPath, "UTF-8")
                    navController.navigate("fullscreen/$imageIndex/$encodedPath")
                }
            )
        }
        composable(
            route = "fullscreen/{imageIndex}/{folderPath}",
            arguments = listOf(
                navArgument("imageIndex") { type = NavType.StringType },
                navArgument("folderPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageIndex = backStackEntry.arguments?.getString("imageIndex")?.toIntOrNull() ?: 0
            val encodedPath = backStackEntry.arguments?.getString("folderPath") ?: ""
            val folderPath = URLDecoder.decode(encodedPath, "UTF-8")

            FullScreenImageScreen(
                initialIndex = imageIndex,
                folderPath = folderPath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
