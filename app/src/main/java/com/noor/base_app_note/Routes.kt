package com.noor.base_app_note

object Routes {
    const val NOTES_LIST = "notes_list"
    const val NOTE_EDITOR = "note_editor/{noteId}"
    const val FOLDER_VIEW = "folder_view/{folderId}"

    const val OCR_SCREEN = "ocr_screen"
    const val SETTINGS = "settings"

    const val HOME = "home"
    fun noteEditor(noteId: String) = "note_editor/$noteId"
    fun folderView(folderId: String) = "folder_view/$folderId"
}
