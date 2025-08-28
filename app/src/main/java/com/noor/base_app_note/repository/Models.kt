package com.noor.base_app_note.repository

import com.noor.base_app_imageviewer_w_ocr.TagColor
import com.noor.base_app_imageviewer_w_ocr.TagType
import java.util.UUID


data class Tag(
    val id: String = UUID.randomUUID().toString(),
    val type: TagType,
    val color: TagColor,
    val customName: String? = null // For user-defined tags beyond enum
) {
    val displayName: String
        get() = customName ?: type.name.lowercase().replaceFirstChar { it.uppercase() }
}


data class NoteFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String, // File system path
    val parentId: String? = null,
    val iconName: String = "folder", // Material icon name
    val color: TagColor = TagColor.BLUE,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)


data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String, // Markdown content
    val filePath: String, // Full file system path to .md file
    val folderId: String? = null,
    val tags: List<Tag> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val wordCount: Int = 0
) {
    val fileName: String
        get() = "$title.md"

    fun updateWordCount(): Note {
        val words = content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return copy(wordCount = words.size)
    }
}

