package com.noor.base_app_note.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.BottomNavigationBar
import com.noor.base_app_note.NotesViewModel
import com.noor.base_app_note.presentation.components.CreateFolderDialog
import com.noor.base_app_note.presentation.components.CreateNoteDialog
import com.noor.base_app_note.presentation.components.EmptyState
import com.noor.base_app_note.presentation.components.ErrorMessage
import com.noor.base_app_note.presentation.components.FolderItem
import com.noor.base_app_note.presentation.components.NoteItem
import com.noor.base_app_note.repository.Note
import com.noor.base_app_note.repository.NoteFolder

@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    navController: NavController,
    onNavigateToEditor: (Note) -> Unit,
    onNavigateToFolder: (NoteFolder) -> Unit,
    onCreateNewNote: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

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

            // Search Bar and scan
            Row {
                IconButton(onClick = { viewModel.scanAndProcessScreenshots() }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan Screenshots")
                }
                IconButton(onClick = { showCreateFolderDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder")
                }
                IconButton(onClick = { showCreateNoteDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Note")
                }

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
            }


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
}
