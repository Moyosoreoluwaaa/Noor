package com.noor.base_app_note.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.noor.base_app_imageviewer_w_ocr.ImageItem
import com.noor.base_app_imageviewer_w_ocr.OCRProcessor
import com.noor.base_app_imageviewer_w_ocr.TagColor
import com.noor.base_app_imageviewer_w_ocr.TagType
import com.noor.base_app_note.repository.Note
import com.noor.base_app_note.repository.NoteFolder
import com.noor.base_app_note.repository.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FixedNoteRepositoryImpl(
    context: Context
) {
    companion object {
        private const val NOTES_DIRECTORY = "MarkdownNotes"
        private const val PREFS_NAME = "notes_prefs"
        private const val KEY_NOTES_LIST = "notes_list"
        private const val TAG = "NoteRepository"
    }

    // Use Documents directory for better external access
    private val notesDir: File by lazy {
        val documentsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        File(documentsDir, NOTES_DIRECTORY).apply {
            if (!exists()) {
                val created = mkdirs()
                Timber.tag(TAG).d("Created notes directory: $created at ${this.absolutePath}")
                // Create a .nomedia file to prevent media scanner from indexing
                try {
                    File(this, ".nomedia").createNewFile()
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Could not create .nomedia file")
                }
            } else {
                Timber.tag(TAG).d("Notes directory exists at ${this.absolutePath}")
            }
        }
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _folders = MutableStateFlow<List<NoteFolder>>(emptyList())
    val folders: StateFlow<List<NoteFolder>> = _folders.asStateFlow()

    init {
        Timber.tag(TAG).d("Repository initialized")
        // Load cached notes from SharedPreferences immediately
        loadCachedNotes()
    }

    suspend fun loadNotes(): List<Note> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Starting to load notes from: ${notesDir.absolutePath}")
                Timber.tag(TAG).d("Directory exists: ${notesDir.exists()}")
                Timber.tag(TAG).d("Directory readable: ${notesDir.canRead()}")

                val notesList = mutableListOf<Note>()

                // Ensure directory exists
                if (!notesDir.exists()) {
                    val created = notesDir.mkdirs()
                    Timber.tag(TAG).d("Created directory: $created")
                }

                // List all files in directory
                val files = notesDir.listFiles()
                Timber.tag(TAG).d("Files in directory: ${files?.size ?: 0}")

                files?.forEach { file ->
                    Timber.tag(TAG)
                        .d("Found file: ${file.name}, isFile: ${file.isFile}, extension: ${file.extension}")
                }

                notesDir.walkTopDown()
                    .filter { it.isFile && it.extension == "md" }
                    .forEach { file ->
                        try {
                            val content = file.readText()
                            val note = parseMarkdownFile(file, content)
                            notesList.add(note)
                            Timber.tag(TAG).d("Loaded note: ${note.title} from ${file.name}")
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, "Error loading note from ${file.name}")
                        }
                    }

                val sortedNotes = notesList.sortedByDescending { it.modifiedAt }
                _notes.value = sortedNotes

                // Cache notes in SharedPreferences
                cacheNotes(sortedNotes)

                Timber.tag(TAG).d("Successfully loaded ${sortedNotes.size} notes")
                sortedNotes
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading notes")
                // Return cached notes on error
                val cachedNotes = _notes.value
                Timber.tag(TAG).d("Returning ${cachedNotes.size} cached notes due to error")
                cachedNotes
            }
        }
    }

    suspend fun loadFolders(): List<NoteFolder> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Loading folders from: ${notesDir.absolutePath}")
                val foldersList = mutableListOf<NoteFolder>()

                if (!notesDir.exists()) {
                    notesDir.mkdirs()
                }

                notesDir.walkTopDown()
                    .filter { it.isDirectory && it != notesDir }
                    .forEach { dir ->
                        val folder = NoteFolder(
                            id = dir.name,
                            name = dir.name,
                            path = dir.absolutePath,
                            createdAt = dir.lastModified(),
                            modifiedAt = dir.lastModified()
                        )
                        foldersList.add(folder)
                        Timber.tag(TAG).d("Found folder: ${folder.name}")
                    }

                _folders.value = foldersList
                Timber.tag(TAG).d("Loaded ${foldersList.size} folders")
                foldersList
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading folders")
                emptyList()
            }
        }
    }

    suspend fun saveNote(note: Note): Result<Note> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Saving note: '${note.title}' to '${note.filePath}'")

                val file = File(note.filePath)
                Timber.tag(TAG).d("File path: ${file.absolutePath}")
                Timber.tag(TAG).d("Parent directory: ${file.parentFile?.absolutePath}")

                file.parentFile?.let { parentDir ->
                    if (!parentDir.exists()) {
                        val created = parentDir.mkdirs()
                        Timber.tag(TAG)
                            .d("Created parent directory: $created at ${parentDir.absolutePath}")
                    }
                }

                val markdownContent = buildMarkdownContent(note)
                Timber.tag(TAG).d("Content length: ${markdownContent.length}")

                file.writeText(markdownContent)
                Timber.tag(TAG).d("File written successfully. Size: ${file.length()} bytes")

                val updatedNote = note.copy(
                    modifiedAt = System.currentTimeMillis()
                ).updateWordCount()

                updateNoteInList(updatedNote)
                Timber.tag(TAG).d("Successfully saved and updated note: ${updatedNote.title}")

                Result.success(updatedNote)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error saving note: ${note.title}")
                Result.failure(e)
            }
        }
    }

    suspend fun createNote(title: String, folderId: String? = null): Result<Note> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Creating note with title: '$title', folderId: $folderId")

                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s-_]"), "").trim()
                val finalTitle = sanitizedTitle.ifBlank { "Untitled" }

                Timber.tag(TAG).d("Sanitized title: '$finalTitle'")

                val targetDir = if (folderId != null) {
                    File(notesDir, folderId)
                } else {
                    notesDir
                }

                if (!targetDir.exists()) {
                    val created = targetDir.mkdirs()
                    Timber.tag(TAG)
                        .d("Created target directory: $created at ${targetDir.absolutePath}")
                }

                val file = File(targetDir, "$finalTitle.md")
                var counter = 1
                var finalFile = file

                while (finalFile.exists()) {
                    finalFile = File(targetDir, "${finalTitle}_$counter.md")
                    counter++
                    Timber.tag(TAG).d("File exists, trying: ${finalFile.name}")
                }

                val initialContent = "# $finalTitle\n\nStart writing your note here..."

                val note = Note(
                    id = UUID.randomUUID().toString(),
                    title = finalTitle,
                    content = initialContent,
                    filePath = finalFile.absolutePath,
                    folderId = folderId,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                ).updateWordCount()

                val markdownContent = buildMarkdownContent(note)
                finalFile.writeText(markdownContent)

                Timber.tag(TAG).d("Created file: ${finalFile.absolutePath}")
                Timber.tag(TAG).d("File exists after creation: ${finalFile.exists()}")
                Timber.tag(TAG).d("File size: ${finalFile.length()}")

                updateNoteInList(note)
                Timber.tag(TAG).d("Successfully created note: ${note.title} with ID: ${note.id}")

                Result.success(note)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error creating note with title: '$title'")
                Result.failure(e)
            }
        }
    }

    suspend fun getNoteById(noteId: String): Note? {
        Timber.tag(TAG).d("Getting note by ID: '$noteId'")
        val note = _notes.value.find { it.id == noteId }
        Timber.tag(TAG).d("Found note: ${note?.title ?: "NOT FOUND"}")
        return note
    }

    suspend fun deleteNote(note: Note): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Deleting note: ${note.title}")
                val file = File(note.filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    Timber.tag(TAG).d("File deleted: $deleted from ${file.absolutePath}")
                }
                removeNoteFromList(note)
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error deleting note: ${note.title}")
                Result.failure(e)
            }
        }
    }

    suspend fun createFolder(name: String, parentId: String? = null): Result<NoteFolder> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.tag(TAG).d("Creating folder: '$name', parentId: $parentId")

                val sanitizedName = name.replace(Regex("[^a-zA-Z0-9\\s-_]"), "").trim()
                val finalName = sanitizedName.ifBlank { "New Folder" }

                val parentDir = if (parentId != null) {
                    File(notesDir, parentId)
                } else {
                    notesDir
                }

                val folderDir = File(parentDir, finalName)
                if (folderDir.exists()) {
                    Timber.tag(TAG).w("Folder already exists: ${folderDir.absolutePath}")
                    return@withContext Result.failure(Exception("Folder already exists"))
                }

                val created = folderDir.mkdirs()
                Timber.tag(TAG).d("Folder created: $created at ${folderDir.absolutePath}")

                val folder = NoteFolder(
                    id = finalName,
                    name = finalName,
                    path = folderDir.absolutePath,
                    parentId = parentId
                )

                updateFolderInList(folder)
                Timber.tag(TAG).d("Successfully created folder: ${folder.name}")

                Result.success(folder)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error creating folder: '$name'")
                Result.failure(e)
            }
        }
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        Timber.tag(TAG).d("Searching notes with query: '$query'")
        return notes.map { notesList ->
            if (query.isBlank()) {
                notesList
            } else {
                val results = notesList.filter { note ->
                    note.title.contains(query, ignoreCase = true) ||
                            note.content.contains(query, ignoreCase = true) ||
                            note.tags.any { it.displayName.contains(query, ignoreCase = true) }
                }
                Timber.tag(TAG).d("Search returned ${results.size} results")
                results
            }
        }
    }

    fun getNotesByFolder(folderId: String?): Flow<List<Note>> {
        Timber.tag(TAG).d("Getting notes by folder: $folderId")
        return notes.map { notesList ->
            val results = notesList.filter { it.folderId == folderId }
            Timber.tag(TAG).d("Found ${results.size} notes in folder")
            results
        }
    }

    fun getStoragePath(): String {
        return notesDir.absolutePath
    }

    private fun loadCachedNotes() {
        try {
            val cachedNotesJson = sharedPrefs.getString(KEY_NOTES_LIST, null)
            if (cachedNotesJson != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
                val cachedNotes: List<Note> = gson.fromJson(cachedNotesJson, type)
                _notes.value = cachedNotes
                Timber.tag(TAG).d("Loaded ${cachedNotes.size} cached notes")
            } else {
                Timber.tag(TAG).d("No cached notes found")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error loading cached notes")
        }
    }

    private fun cacheNotes(notesList: List<Note>) {
        try {
            val notesJson = gson.toJson(notesList)
            sharedPrefs.edit { putString(KEY_NOTES_LIST, notesJson) }
            Timber.tag(TAG).d("Cached ${notesList.size} notes")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error caching notes")
        }
    }

    private fun parseMarkdownFile(file: File, content: String): Note {
        val lines = content.lines()
        val title = lines.firstOrNull { it.startsWith("# ") }
            ?.substring(2)?.trim()
            ?: file.nameWithoutExtension

        // Extract tags from markdown comments
        val tagPattern = "<!-- Tags: (.+) -->".toRegex()
        val tagMatch = tagPattern.find(content)
        val tags = tagMatch?.groupValues?.get(1)?.split(", ")?.mapNotNull { tagName ->
            TagType.entries.find { it.name.equals(tagName.trim(), ignoreCase = true) }?.let { tagType ->
                Tag(type = tagType, color = TagColor.entries.toTypedArray().random())
            }
        } ?: emptyList()

        return Note(
            id = file.nameWithoutExtension + "_" + file.lastModified(),
            title = title,
            content = content,
            filePath = file.absolutePath,
            modifiedAt = file.lastModified(),
            createdAt = file.lastModified(),
            tags = tags
        ).updateWordCount()
    }

    private fun buildMarkdownContent(note: Note): String {
        val tagSection = if (note.tags.isNotEmpty()) {
            "\n<!-- Tags: ${note.tags.joinToString(", ") { it.displayName }} -->\n"
        } else ""

        return note.content + tagSection
    }

    private fun updateNoteInList(note: Note) {
        val currentNotes = _notes.value.toMutableList()
        val existingIndex = currentNotes.indexOfFirst { it.id == note.id }

        if (existingIndex != -1) {
            currentNotes[existingIndex] = note
            Timber.tag(TAG).d("Updated existing note in list: ${note.title}")
        } else {
            currentNotes.add(note)
            Timber.tag(TAG).d("Added new note to list: ${note.title}")
        }

        val updatedList = currentNotes.sortedByDescending { it.modifiedAt }
        _notes.value = updatedList

        // Cache updated notes
        cacheNotes(updatedList)
    }

    private fun removeNoteFromList(note: Note) {
        val updatedList = _notes.value.filter { it.id != note.id }
        _notes.value = updatedList
        cacheNotes(updatedList)
        Timber.tag(TAG).d("Removed note from list: ${note.title}")
    }

    private fun updateFolderInList(folder: NoteFolder) {
        val currentFolders = _folders.value.toMutableList()
        val existingIndex = currentFolders.indexOfFirst { it.id == folder.id }

        if (existingIndex != -1) {
            currentFolders[existingIndex] = folder
        } else {
            currentFolders.add(folder)
        }

        _folders.value = currentFolders
        Timber.tag(TAG).d("Updated folder in list: ${folder.name}")
    }
}

