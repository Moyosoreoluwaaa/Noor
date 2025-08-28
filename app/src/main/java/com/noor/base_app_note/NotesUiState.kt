package com.noor.base_app_note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noor.base_app_imageviewer_w_ocr.TagColor
import com.noor.base_app_imageviewer_w_ocr.TagType
import com.noor.base_app_imageviewer_w_ocr_bg_scan.data.repository.OCRRepositoryImpl
import com.noor.base_app_note.data.repository.FixedNoteRepositoryImpl
import com.noor.base_app_note.repository.Note
import com.noor.base_app_note.repository.NoteFolder
import com.noor.base_app_note.repository.Tag
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


// File: NotesUiState.kt
// Package: com.noteapp.presentation.viewmodel
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val folders: List<NoteFolder> = emptyList(),
    val currentFolder: NoteFolder? = null,
    val selectedNote: Note? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSearching: Boolean = false
)

// File: EditorUiState.kt
// Package: com.noteapp.presentation.viewmodel
data class EditorUiState(
    val note: Note? = null,
    val content: String = "",
    val title: String = "",
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val error: String? = null,
    val wordCount: Int = 0,
    val availableTags: List<Tag> = emptyList(),
    val selectedTags: List<Tag> = emptyList()
)

// File: FolderUiState.kt
// Package: com.noteapp.presentation.viewmodel
data class FolderUiState(
    val folders: List<NoteFolder> = emptyList(),
    val currentPath: List<NoteFolder> = emptyList(),
    val isCreatingFolder: Boolean = false,
    val error: String? = null
)

// File: NotesViewModel.kt
// Package: com.noteapp.presentation.viewmodel
class NotesViewModel(
    private val repository: FixedNoteRepositoryImpl,
    private val ocrRepository: OCRRepositoryImpl
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }


    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val notes = repository.loadNotes()
                val folders = repository.loadFolders()
                
                _uiState.value = _uiState.value.copy(
                    notes = notes,
                    folders = folders,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }

    fun scanAndProcessScreenshots() {
        viewModelScope.launch {
            Timber.d("Starting scan and process workflow")
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Always scan first
            val scanResult = ocrRepository.scanForNewScreenshots()
            Timber.d("Scan result: newFound=${scanResult.newImagesFound}, totalPending=${scanResult.totalPending}")

            // Process if we have ANY pending images (new OR existing)
            if (scanResult.totalPending > 0) {
                Timber.d("Processing pending images. Total: ${scanResult.totalPending}")
                val processingResult = ocrRepository.processPendingImages()

                Timber.d("Processing complete: processed=${processingResult.processed}, failed=${processingResult.failed}, success=${processingResult.success}")
                if (processingResult.success) {
                    Timber.d("Processing succeeded, reloading data")
                    loadData()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    Timber.e("Processing failed with message: ${processingResult.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = processingResult.message
                    )
                }
            } else {
                Timber.d("No new or pending images found. Hiding loading state.")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = scanResult.message
                )
            }
            Timber.d("Scan and process workflow finished")
        }
    }

    fun searchNotes(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSearching = query.isNotEmpty()
        )
        
        if (query.isNotEmpty()) {
            viewModelScope.launch {
                repository.searchNotes(query).collect { searchResults ->
                    _uiState.value = _uiState.value.copy(notes = searchResults)
                }
            }
        } else {
            loadData()
        }
    }
    
    fun selectNote(note: Note) {
        _uiState.value = _uiState.value.copy(selectedNote = note)
    }
    
    fun navigateToFolder(folder: NoteFolder?) {
        _uiState.value = _uiState.value.copy(currentFolder = folder)
        
        viewModelScope.launch {
            repository.getNotesByFolder(folder?.id).collect { folderNotes ->
                _uiState.value = _uiState.value.copy(notes = folderNotes)
            }
        }
    }
    
    fun createNote(title: String, folderId: String? = null) {
        viewModelScope.launch {
            repository.createNote(title, folderId).fold(
                onSuccess = { newNote ->
                    _uiState.value = _uiState.value.copy(selectedNote = newNote)
                    loadData() // Refresh the list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to create note: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note).fold(
                onSuccess = {
                    if (_uiState.value.selectedNote?.id == note.id) {
                        _uiState.value = _uiState.value.copy(selectedNote = null)
                    }
                    loadData() // Refresh the list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete note: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun createFolder(name: String, parentId: String? = null) {
        viewModelScope.launch {
            repository.createFolder(name, parentId).fold(
                onSuccess = {
                    loadData() // Refresh the list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to create folder: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// File: FixedEditorViewModel.kt
// Package: com.noteapp.presentation.viewmodel
class FixedEditorViewModel(
    private val repository: FixedNoteRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var originalContent = ""
    private var saveJob: Job? = null

    suspend fun loadNote(noteId: String) {
        viewModelScope.launch {
            try {
                val note = repository.getNoteById(noteId)
                if (note != null) {
                    _uiState.value = EditorUiState(
                        note = note,
                        content = note.content,
                        title = note.title,
                        wordCount = note.wordCount,
                        selectedTags = note.tags,
                        availableTags = generateAvailableTags()
                    )
                    originalContent = note.content
                    Timber.tag("EditorViewModel").d("Loaded note: ${note.title}")
                } else {
                    Timber.tag("EditorViewModel").e("Note not found: $noteId")
                    _uiState.value = _uiState.value.copy(
                        error = "Note not found"
                    )
                }
            } catch (e: Exception) {
                Timber.tag("EditorViewModel").e(e, "Error loading note")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load note: ${e.message}"
                )
            }
        }
    }

    fun updateContent(newContent: String) {
        val hasChanges = newContent != originalContent
        val wordCount = newContent.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }.size

        _uiState.value = _uiState.value.copy(
            content = newContent,
            hasUnsavedChanges = hasChanges,
            wordCount = wordCount
        )
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(
            title = newTitle,
            hasUnsavedChanges = true
        )
    }

    fun saveNote() {
        val currentState = _uiState.value
        val note = currentState.note ?: return

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            val updatedNote = note.copy(
                title = currentState.title,
                content = currentState.content,
                tags = currentState.selectedTags,
                modifiedAt = System.currentTimeMillis()
            ).updateWordCount()

            repository.saveNote(updatedNote).fold(
                onSuccess = { savedNote ->
                    originalContent = savedNote.content
                    _uiState.value = _uiState.value.copy(
                        note = savedNote,
                        isSaving = false,
                        hasUnsavedChanges = false,
                        wordCount = savedNote.wordCount
                    )
                    Timber.tag("EditorViewModel").d("Note saved successfully: ${savedNote.title}")
                },
                onFailure = { error ->
                    Timber.tag("EditorViewModel").e(error, "Failed to save note")
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Failed to save note: ${error.message}"
                    )
                }
            )
        }
    }

    fun addTag(tag: Tag) {
        val currentTags = _uiState.value.selectedTags.toMutableList()
        if (!currentTags.contains(tag)) {
            currentTags.add(tag)
            _uiState.value = _uiState.value.copy(
                selectedTags = currentTags,
                hasUnsavedChanges = true
            )
        }
    }

    fun removeTag(tag: Tag) {
        val currentTags = _uiState.value.selectedTags.toMutableList()
        currentTags.remove(tag)
        _uiState.value = _uiState.value.copy(
            selectedTags = currentTags,
            hasUnsavedChanges = true
        )
    }

    fun toggleFavorite() {
        val note = _uiState.value.note ?: return
        val updatedNote = note.copy(isFavorite = !note.isFavorite)

        viewModelScope.launch {
            repository.saveNote(updatedNote).fold(
                onSuccess = { savedNote ->
                    _uiState.value = _uiState.value.copy(note = savedNote)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update favorite: ${error.message}"
                    )
                }
            )
        }
    }

    private fun generateAvailableTags(): List<Tag> {
        return TagType.entries.toTypedArray().mapIndexed { index, tagType ->
            Tag(
                type = tagType,
                color = TagColor.entries[index % TagColor.entries.size]
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        saveJob?.cancel()
    }
}
