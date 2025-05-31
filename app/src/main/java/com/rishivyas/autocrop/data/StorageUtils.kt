package com.rishivyas.autocrop.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for storage operations like saving and loading images
 */
object StorageUtils {
    
    private const val PROCESSED_FACES_DIR = "ProcessedFaces"
    private const val TEMP_IMAGES_DIR = "Pictures"
    
    /**
     * Creates a temporary image file for camera capture
     */
    fun createImageFile(context: Context): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(TEMP_IMAGES_DIR)
        return try {
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Saves a processed image to storage
     */
    fun saveProcessedImage(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val imagesDir = context.getExternalFilesDir(PROCESSED_FACES_DIR)
            if (!imagesDir?.exists()!!) {
                imagesDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File(imagesDir, "face_$timestamp.jpg")

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Notify media scanner to make the image visible in gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

            Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    /**
     * Loads all saved images from storage
     */
    fun loadSavedImages(context: Context): List<File> {
        val dir = context.getExternalFilesDir(PROCESSED_FACES_DIR)
        return dir?.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
} 