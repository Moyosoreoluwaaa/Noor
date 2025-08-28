package com.noor.base_app_note.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noor.base_app_note.FixedEditorViewModel
import com.noor.base_app_note.formatTimestamp
import com.noor.base_app_note.presentation.components.TagChipRemovable
import com.noor.base_app_note.presentation.components.TagSelectorDialog
import kotlinx.coroutines.delay
import timber.log.Timber

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