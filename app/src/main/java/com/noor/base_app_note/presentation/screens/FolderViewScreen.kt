package com.noor.base_app_note.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noor.base_app_note.NotesViewModel
import com.noor.base_app_note.presentation.components.EmptyState
import com.noor.base_app_note.presentation.components.NoteItem
import com.noor.base_app_note.repository.Note

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
