package com.noor.base_app_1.data// ImageRepository.kt
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

class ImageRepository(private val context: Context) {
    
    fun getAllImageFolders(): List<ImageFolder> {
        val folders = mutableMapOf<String, MutableList<ImageItem>>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA
        )
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateColumn)
                val data = cursor.getString(dataColumn)
                
                val file = File(data)
                val folderPath = file.parent ?: continue
                val folderName = File(folderPath).name
                
                val imageItem = ImageItem(
                    id = id,
                    uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()).toString(),
                    displayName = name,
                    size = size,
                    dateModified = dateModified,
                    folderName = folderName,
                    folderPath = folderPath
                )
                
                folders.getOrPut(folderPath) { mutableListOf() }.add(imageItem)
            }
        }
        
        return folders.map { (path, images) ->
            ImageFolder(
                name = File(path).name,
                path = path,
                images = images,
                coverImage = images.firstOrNull()
            )
        }.sortedBy { it.name }
    }
    
    fun getImagesInFolder(folderPath: String): List<ImageItem> {
        return getAllImageFolders()
            .find { it.path == folderPath }
            ?.images ?: emptyList()
    }
}
