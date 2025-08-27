package com.noor.base_app_note

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.noor.base_app_note.repository.Note
import com.noor.base_app_note.repository.NoteFolder
import com.noor.base_app_note.repository.PermissionHandler
import com.noor.base_app_note.repository.Tag
import kotlinx.coroutines.delay
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// File: Routes.kt
// Package: com.noteapp.presentation.navigation
object Routes {
    const val NOTES_LIST = "notes_list"
    const val NOTE_EDITOR = "note_editor/{noteId}"
    const val FOLDER_VIEW = "folder_view/{folderId}"
    
    fun noteEditor(noteId: String) = "note_editor/$noteId"
    fun folderView(folderId: String) = "folder_view/$folderId"
}

// File: NotesNavigation.kt
// Package: com.noteapp.presentation.navigation
//@Composable
//fun NotesNavigation(
//    navController: NavHostController,
//    notesViewModel: NotesViewModel,
//    editorViewModel: EditorViewModel
//) {
//    NavHost(
//        navController = navController,
//        startDestination = Routes.NOTES_LIST
//    ) {
//        composable(Routes.NOTES_LIST) {
//            NotesListScreen(
//                viewModel = notesViewModel,
//                onNavigateToEditor = { note ->
//                    navController.navigate(Routes.noteEditor(note.id))
//                },
//                onNavigateToFolder = { folder ->
//                    navController.navigate(Routes.folderView(folder.id))
//                }
//            )
//        }
//
//        composable(
//            route = Routes.NOTE_EDITOR,
//            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
//        ) { backStackEntry ->
//            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
//            NoteEditorScreen(
//                viewModel = editorViewModel,
//                noteId = noteId,
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        composable(
//            route = Routes.FOLDER_VIEW,
//            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
//        ) { backStackEntry ->
//            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
//            FolderViewScreen(
//                viewModel = notesViewModel,
//                folderId = folderId,
//                onNavigateToEditor = { note ->
//                    navController.navigate(Routes.noteEditor(note.id))
//                },
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }
//    }
//}

// File: PermissionScreen.kt
// Package: com.noteapp.presentation.permissions
@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var showRationale by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        val permissionHandler = PermissionHandler(context)

        if (permissionHandler.hasStoragePermissions()) {
            onPermissionGranted()
        } else {
            showRationale = activity?.let { permissionHandler.shouldShowRationale(it) } == true
            if (!permissionRequested) {
                permissionRequested = true
                permissionLauncher.launch(PermissionHandler.REQUIRED_PERMISSIONS)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Storage Access Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This app needs access to storage to save and manage your markdown notes as files on your device.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                permissionLauncher.launch(PermissionHandler.REQUIRED_PERMISSIONS)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onPermissionDenied) {
            Text("Skip for Now")
        }

        if (showRationale) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Why we need this permission:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Save notes as .md files you can access\n• Create folders for organization\n• Allow external file manager access",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}



// File: UpdatedNotesListScreen.kt
// Package: com.noteapp.presentation.screens
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNavigateToEditor: (Note) -> Unit,
    onNavigateToFolder: (NoteFolder) -> Unit,
    onCreateNewNote: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar with Search and Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.currentFolder != null) uiState.currentFolder!!.name else "Notes",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(onClick = { showCreateFolderDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder")
                }
                IconButton(onClick = { showCreateNoteDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Note")
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::searchNotes,
            label = { Text("Search notes...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchNotes("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        when {
            uiState.isLoading -> {
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
                            text = "Loading your notes...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            uiState.error != null -> {
                ErrorMessage(
                    message = uiState.error!!,
                    onRetry = viewModel::loadData,
                    onDismiss = viewModel::clearError
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show folders first
                    items(uiState.folders) { folder ->
                        FolderItem(
                            folder = folder,
                            onClick = { onNavigateToFolder(folder) }
                        )
                    }

                    // Then show notes
                    items(uiState.notes) { note ->
                        NoteItem(
                            note = note,
                            onClick = { onNavigateToEditor(note) },
                            onDelete = { viewModel.deleteNote(note) }
                        )
                    }

                    // Empty state
                    if (uiState.notes.isEmpty() && uiState.folders.isEmpty()) {
                        item {
                            EmptyState(
                                message = if (uiState.isSearching) "No notes found" else "No notes yet",
                                actionText = "Create Note",
                                onAction = { showCreateNoteDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Note Dialog
    if (showCreateNoteDialog) {
        CreateNoteDialog(
            onConfirm = { title ->
                viewModel.createNote(title, uiState.currentFolder?.id)
                showCreateNoteDialog = false
            },
            onDismiss = { showCreateNoteDialog = false }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { name ->
                viewModel.createFolder(name, uiState.currentFolder?.id)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }
}


@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = note.content.take(100) + if (note.content.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.isFavorite) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "${note.wordCount} words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = formatTimestamp(note.modifiedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (note.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(note.tags.take(3)) { tag ->
                                TagChip(tag = tag)
                            }
                            if (note.tags.size > 3) {
                                item {
                                    Text(
                                        text = "+${note.tags.size - 3}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun FolderItem(
    folder: NoteFolder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = folder.color.color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = "Folder",
                tint = folder.color.color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = formatTimestamp(folder.modifiedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open folder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TagChip(tag: Tag) {
    Surface(
        color = tag.color.color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = tag.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = tag.color.color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onAction) {
            Text(actionText)
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

////////////////

@Composable
fun NoteEditorScreen(
    viewModel: FixedEditorViewModel,
    noteId: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTagSelector by remember { mutableStateOf(false) }

    // Load note when screen is first displayed
    LaunchedEffect(noteId) {
        if (noteId.isNotBlank() && noteId != "new") {
            viewModel.loadNote(noteId)
        } else {
            // Handle new note creation case
            Timber.tag("NoteEditorScreen").d("Creating new note")
        }
    }

    // Auto-save every 5 seconds when there are unsaved changes
    LaunchedEffect(uiState.hasUnsavedChanges) {
        if (uiState.hasUnsavedChanges) {
            delay(5000) // Wait 5 seconds
            if (!(uiState.isSaving)) {
                viewModel.saveNote()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                // Save before navigating back if there are changes
                if (uiState.hasUnsavedChanges) {
                    viewModel.saveNote()
                }
                onNavigateBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unsaved changes indicator
                if (uiState.hasUnsavedChanges) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Saving indicator
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                }

                // Favorite button
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        if (uiState.note?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (uiState.note?.isFavorite == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tags button
                IconButton(onClick = { showTagSelector = true }) {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Tags")
                }

                // Manual save button
                IconButton(
                    onClick = viewModel::saveNote,
                    enabled = uiState.hasUnsavedChanges && !uiState.isSaving
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save",
                        tint = if (uiState.hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Title Field
        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            singleLine = true,
            placeholder = { Text("Enter note title...") }
        )

        // Tags Display
        if (uiState.selectedTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(uiState.selectedTags) { tag ->
                    TagChipRemovable(
                        tag = tag,
                        onRemove = { viewModel.removeTag(tag) }
                    )
                }
            }
        }

        // Metadata Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.wordCount} words",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.note?.modifiedAt != null) {
                Text(
                    text = "Modified ${formatTimestamp(uiState.note!!.modifiedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Content Editor
        OutlinedTextField(
            value = uiState.content,
            onValueChange = viewModel::updateContent,
            label = { Text("Content") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            minLines = 20,
            placeholder = { Text("Start writing your markdown note...\n\n# Heading 1\n## Heading 2\n\n**Bold text**\n*Italic text*\n\n- List item 1\n- List item 2") }
        )

        // Error Display
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = viewModel::clearError,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    // Tag Selector Dialog
    if (showTagSelector) {
        TagSelectorDialog(
            availableTags = uiState.availableTags,
            selectedTags = uiState.selectedTags,
            onTagToggle = { tag ->
                if (uiState.selectedTags.contains(tag)) {
                    viewModel.removeTag(tag)
                } else {
                    viewModel.addTag(tag)
                }
            },
            onDismiss = { showTagSelector = false }
        )
    }
}

// File: UpdatedNotesNavigation.kt
// Package: com.noteapp.presentation.navigation
@Composable
fun NotesNavigation(
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    editorViewModel: FixedEditorViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.NOTES_LIST
    ) {
        composable(Routes.NOTES_LIST) {
            NotesListScreen(
                viewModel = notesViewModel,
                onNavigateToEditor = { note ->
                    navController.navigate(Routes.noteEditor(note.id))
                },
                onNavigateToFolder = { folder ->
                    navController.navigate(Routes.folderView(folder.id))
                }
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
}

// Utility function for timestamp formatting
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val date = Date(timestamp)
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        }
    }
}

//////////////////
// File: NoteEditorScreen.kt
// Package: com.noteapp.presentation.screens
//@Composable
//fun NoteEditorScreen(
//    viewModel: EditorViewModel,
//    noteId: String,
//    onNavigateBack: () -> Unit
//) {
//    val uiState by viewModel.uiState.collectAsState()
//    var showTagSelector by remember { mutableStateOf(false) }
//
//    LaunchedEffect(noteId) {
//        // Load note based on noteId from the repository
//        // This would typically involve finding the note by ID
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        // Top Bar
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = onNavigateBack) {
//                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
//            }
//
//            Row(
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                if (uiState.hasUnsavedChanges) {
//                    Text(
//                        text = "•",
//                        color = MaterialTheme.colorScheme.primary,
//                        style = MaterialTheme.typography.titleLarge
//                    )
//                }
//
//                if (uiState.isSaving) {
//                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
//                }
//
//                IconButton(onClick = viewModel::toggleFavorite) {
//                    Icon(
//                        if (uiState.note?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
//                        contentDescription = "Favorite",
//                        tint = if (uiState.note?.isFavorite == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//
//                IconButton(onClick = { showTagSelector = true }) {
//                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Tags")
//                }
//
//                IconButton(onClick = viewModel::saveNote) {
//                    Icon(Icons.Default.Save, contentDescription = "Save")
//                }
//            }
//        }
//
//        // Title Field
//        OutlinedTextField(
//            value = uiState.title,
//            onValueChange = viewModel::updateTitle,
//            label = { Text("Title") },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 8.dp),
//            singleLine = true
//        )
//
//        // Tags
//        if (uiState.selectedTags.isNotEmpty()) {
//            LazyRow(
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                modifier = Modifier.padding(bottom = 8.dp)
//            ) {
//                items(uiState.selectedTags) { tag ->
//                    TagChipRemovable(
//                        tag = tag,
//                        onRemove = { viewModel.removeTag(tag) }
//                    )
//                }
//            }
//        }
//
//        // Word Count
//        Text(
//            text = "${uiState.wordCount} words",
//            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//            modifier = Modifier.padding(bottom = 8.dp)
//        )
//
//        // Content Editor
//        OutlinedTextField(
//            value = uiState.content,
//            onValueChange = viewModel::updateContent,
//            label = { Text("Content") },
//            modifier = Modifier
//                .fillMaxWidth()
//                .weight(1f),
//            minLines = 20,
//            placeholder = { Text("Start writing your markdown note...") }
//        )
//
//        // Error Display
//        uiState.error?.let { error ->
//            Text(
//                text = error,
//                color = MaterialTheme.colorScheme.error,
//                style = MaterialTheme.typography.bodySmall,
//                modifier = Modifier.padding(top = 8.dp)
//            )
//        }
//    }
//
//    // Tag Selector Dialog
//    if (showTagSelector) {
//        TagSelectorDialog(
//            availableTags = uiState.availableTags,
//            selectedTags = uiState.selectedTags,
//            onTagToggle = { tag ->
//                if (uiState.selectedTags.contains(tag)) {
//                    viewModel.removeTag(tag)
//                } else {
//                    viewModel.addTag(tag)
//                }
//            },
//            onDismiss = { showTagSelector = false }
//        )
//    }
//}

@Composable
fun TagChipRemovable(
    tag: Tag,
    onRemove: () -> Unit
) {
    Surface(
        color = tag.color.color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = tag.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = tag.color.color
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier.size(12.dp),
                    tint = tag.color.color
                )
            }
        }
    }
}

// File: DialogComponents.kt
// Package: com.noteapp.presentation.components
@Composable
fun CreateNoteDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Note") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Note title") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim())
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TagSelectorDialog(
    availableTags: List<Tag>,
    selectedTags: List<Tag>,
    onTagToggle: (Tag) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tags") },
        text = {
            LazyColumn {
                items(availableTags) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagToggle(tag) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedTags.contains(tag),
                            onCheckedChange = { onTagToggle(tag) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = tag.color.color,
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(text = tag.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// File: FolderViewScreen.kt
// Package: com.noteapp.presentation.screens
@Composable
fun FolderViewScreen(
    viewModel: NotesViewModel,
    folderId: String,
    onNavigateToEditor: (Note) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(folderId) {
        // Load folder content
        viewModel.navigateToFolder(uiState.folders.find { it.id == folderId })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = uiState.currentFolder?.name ?: "Folder",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        // Folder Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.notes) { note ->
                NoteItem(
                    note = note,
                    onClick = { onNavigateToEditor(note) },
                    onDelete = { viewModel.deleteNote(note) }
                )
            }

            if (uiState.notes.isEmpty()) {
                item {
                    EmptyState(
                        message = "This folder is empty",
                        actionText = "Create Note",
                        onAction = {
                            viewModel.createNote("Untitled", folderId)
                        }
                    )
                }
            }
        }
    }
}

// Utility function for timestamp formatting
//private fun formatTimestamp(timestamp: Long): String {
//    val now = System.currentTimeMillis()
//    val diff = now - timestamp
//
//    return when {
//        diff < 60_000 -> "Just now"
//        diff < 3600_000 -> "${diff / 60_000}m ago"
//        diff < 86400_000 -> "${diff / 3600_000}h ago"
//        diff < 604800_000 -> "${diff / 86400_000}d ago"
//        else -> {
//            val date = java.util.Date(timestamp)
//            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(date)
//        }
//    }
//}