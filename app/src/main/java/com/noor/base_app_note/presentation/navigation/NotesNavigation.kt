package com.noor.base_app_note.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.noor.base_app_imageviewer_w_ocr.OCRProcessor
import com.noor.base_app_imageviewer_w_ocr_bg_scan.data.repository.OCRRepositoryImpl
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.screens.OCRScreen
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.viewmodels.OCRViewModel
import com.noor.base_app_note.FixedEditorViewModel
import com.noor.base_app_note.NotesViewModel
import com.noor.base_app_note.Routes
import com.noor.base_app_note.data.repository.FixedNoteRepositoryImpl
import com.noor.base_app_note.presentation.screens.FolderViewScreen
import com.noor.base_app_note.presentation.screens.NoteEditorScreen
import com.noor.base_app_note.presentation.screens.NotesListScreen

@Composable
fun NotesNavigation(
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    editorViewModel: FixedEditorViewModel
) {
    // Manual DI - Create repository instances
    val context = LocalContext.current
    val noteRepository = remember { FixedNoteRepositoryImpl(context) }
    val ocrProcessor = remember { OCRProcessor(context) }
    val ocrRepository = remember { OCRRepositoryImpl(context, noteRepository, ocrProcessor) }


    NavHost(
        navController = navController,
        startDestination = Routes.NOTES_LIST
    ) {
//
//        composable(Routes.HOME) {
//            HomeScreen(navController = navController)
//        }
        
        composable(Routes.NOTES_LIST) {
            NotesListScreen(
                viewModel = notesViewModel,
                onNavigateToEditor = { note ->
                    navController.navigate(Routes.noteEditor(note.id))
                },
                onNavigateToFolder = { folder ->
                    navController.navigate(Routes.folderView(folder.id))
                },
                navController = navController
            )
        }

        composable(Routes.OCR_SCREEN) {
            val ocrViewModel = remember { OCRViewModel(ocrRepository) }
            OCRScreen(
                navController = navController,
                viewModel = ocrViewModel
            )
        }

        composable(
            route = Routes.NOTE_EDITOR,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NoteEditorScreen(
                viewModel = editorViewModel,
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.FOLDER_VIEW,
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            FolderViewScreen(
                viewModel = notesViewModel,
                folderId = folderId,
                onNavigateToEditor = { note ->
                    navController.navigate(Routes.noteEditor(note.id))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
//
//        composable(
//            route = Routes.NOTE_DETAIL_WITH_ID,
//            arguments = listOf(
//                navArgument("noteId") {
//                    type = NavType.StringType
//                }
//            )
//        ) { backStackEntry ->
//            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
//            // Your existing note detail screen
//            NoteDetailScreen(
//                navController = navController,
//                noteId = noteId
//            )
//        }
}
