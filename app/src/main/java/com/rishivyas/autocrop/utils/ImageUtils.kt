package com.rishivyas.autocrop.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Utility class for image processing operations
 */
object ImageUtils {
    
    // Configure Face Detector with specified options
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()

        FaceDetection.getClient(options)
    }
    
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
     * Detects faces in a bitmap
     */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            faceDetector.process(image).await()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Crops a face from a bitmap with padding
     */
    fun cropFace(bitmap: Bitmap, face: Face, paddingPercent: Float = 0.2f): Bitmap? {
        val boundingBox = face.boundingBox
        
        // Ensure the bounding box is within the bitmap bounds
        val left = boundingBox.left.coerceIn(0, bitmap.width)
        val top = boundingBox.top.coerceIn(0, bitmap.height)
        val right = boundingBox.right.coerceIn(0, bitmap.width)
        val bottom = boundingBox.bottom.coerceIn(0, bitmap.height)
        
        // Add padding around the face
        val padding = (boundingBox.width() * paddingPercent).toInt()
        val paddedLeft = (left - padding).coerceIn(0, bitmap.width)
        val paddedTop = (top - padding).coerceIn(0, bitmap.height)
        val paddedRight = (right + padding).coerceIn(0, bitmap.width)
        val paddedBottom = (bottom + padding).coerceIn(0, bitmap.height)
        
        // Create the cropped bitmap
        return try {
            Bitmap.createBitmap(
                bitmap,
                paddedLeft,
                paddedTop,
                paddedRight - paddedLeft,
                paddedBottom - paddedTop
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Draws eye contours on a bitmap
     */
    fun drawEyeContoursOnBitmap(
        bitmap: Bitmap,
        face: Face,
        cropOffsetX: Int,
        cropOffsetY: Int
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Draw left eye contour points
        face.getContour(FaceContour.LEFT_EYE)?.let { contour ->
            contour.points.forEach { point ->
                val x = point.x - cropOffsetX
                val y = point.y - cropOffsetY
                canvas.drawCircle(x, y, 2f, paint)
            }
        }
        
        // Draw right eye contour points
        face.getContour(FaceContour.RIGHT_EYE)?.let { contour ->
            contour.points.forEach { point ->
                val x = point.x - cropOffsetX
                val y = point.y - cropOffsetY
                canvas.drawCircle(x, y, 2f, paint)
            }
        }
        
        return mutableBitmap
    }
} 