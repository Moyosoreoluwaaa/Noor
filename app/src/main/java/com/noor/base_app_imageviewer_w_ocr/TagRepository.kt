package com.noor.base_app_imageviewer_w_ocr

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TagRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("image_tags", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getTagsForImage(imageId: Long): List<ImageTag> {
        val json = prefs.getString("tags_$imageId", null) ?: return emptyList()
        val type = object : TypeToken<List<ImageTag>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addTagToImage(imageId: Long, tag: ImageTag) {
        val currentTags = getTagsForImage(imageId).toMutableList()
        if (!currentTags.any { it.type == tag.type }) {
            currentTags.add(tag)
            saveTagsForImage(imageId, currentTags)
        }
    }

    fun removeTagFromImage(imageId: Long, tagType: TagType) {
        val currentTags = getTagsForImage(imageId).toMutableList()
        currentTags.removeAll { it.type == tagType }
        saveTagsForImage(imageId, currentTags)
    }

    private fun saveTagsForImage(imageId: Long, tags: List<ImageTag>) {
        val json = gson.toJson(tags)
        prefs.edit().putString("tags_$imageId", json).apply()
    }

    fun getAllAvailableTags(): List<ImageTag> {
        return TagType.values().map { type ->
            ImageTag(type = type, color = getDefaultColorForTag(type))
        }
    }

    fun getAllTaggedImages(): Map<Long, List<ImageTag>> {
        val allTags = mutableMapOf<Long, List<ImageTag>>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("tags_") && value is String) {
                val imageId = key.removePrefix("tags_").toLongOrNull()
                if (imageId != null) {
                    val type = object : TypeToken<List<ImageTag>>() {}.type
                    try {
                        val tags: List<ImageTag> = gson.fromJson(value, type) ?: emptyList()
                        if (tags.isNotEmpty()) {
                            allTags[imageId] = tags
                        }
                    } catch (e: Exception) {
                        // Skip corrupted entries
                    }
                }
            }
        }
        return allTags
    }

    fun clearAllTags() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("tags_") }.forEach { key ->
            editor.remove(key)
        }
        editor.apply()
    }

    fun getImagesByTag(tagType: TagType): List<Long> {
        return getAllTaggedImages()
            .filter { (_, tags) -> tags.any { it.type == tagType } }
            .keys.toList()
    }
}