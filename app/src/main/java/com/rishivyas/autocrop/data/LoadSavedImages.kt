package com.rishivyas.autocrop.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class to load saved images with thumbnails
 */
object LoadSavedImages {
    
    // Thumbnail dimensions
    private const val THUMBNAIL_SIZE = 48
    
    /**
     * Loads all saved images with thumbnails
     */
    suspend fun loadSavedImagesWithThumbnails(context: Context): List<SavedImage> = withContext(Dispatchers.IO) {
        val files = StorageUtils.loadSavedImages(context)
        files.map { file ->
            SavedImage(
                file = file,
                thumbnail = createThumbnail(file.absolutePath)
            )
        }
    }
    
    /**
     * Creates a thumbnail from an image file
     */
    private fun createThumbnail(filePath: String): Bitmap? {
        return try {
            val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
            bitmap.scale(THUMBNAIL_SIZE, THUMBNAIL_SIZE, filter = true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 