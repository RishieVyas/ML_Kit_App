package com.rishivyas.autocrop.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.face.Face
import com.rishivyas.faceprocessor.FaceProcessor
import com.rishivyas.faceprocessor.ProcessingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class for image processing operations that uses the FaceProcessor library
 */
object ImageUtils {
    
    // Create an instance of the FaceProcessor from the library
    private val faceProcessor = FaceProcessor()
    
    /**
     * Decodes a URI to a Bitmap
     */
    fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Detects faces in a bitmap using the FaceProcessor library
     */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> = withContext(Dispatchers.IO) {
        faceProcessor.detectFaces(bitmap)
    }
    
    /**
     * Crops a face from a bitmap with padding using the FaceProcessor library
     */
    fun cropFace(bitmap: Bitmap, face: Face, paddingPercent: Float = 0.2f): Bitmap? {
        return faceProcessor.cropFace(bitmap, face, paddingPercent)
    }
    
    /**
     * Draws eye contours on a bitmap using the FaceProcessor library
     */
    fun drawEyeContoursOnBitmap(
        bitmap: Bitmap,
        face: Face,
        cropOffsetX: Int,
        cropOffsetY: Int
    ): Bitmap {
        return faceProcessor.drawEyeContours(bitmap, face, cropOffsetX, cropOffsetY)
    }
    
    /**
     * Process a bitmap fully (detect, crop, draw) using the FaceProcessor library
     */
    suspend fun processBitmap(bitmap: Bitmap): ProcessingResult {
        return faceProcessor.process(bitmap)
    }
} 