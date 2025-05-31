package com.rishivyas.faceprocessor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * A processor for face detection, cropping, and eye contour drawing using Google ML Kit.
 * This is the main class of the face processor library.
 */
class FaceProcessor {
    
    companion object {
        // Default padding percentage for face cropping
        const val DEFAULT_PADDING_PERCENT = 0.2f
        
        // Default eye contour point size
        const val DEFAULT_EYE_POINT_SIZE = 2f
        
        // Default eye contour color
        val DEFAULT_EYE_CONTOUR_COLOR = Color.RED
    }
    
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
     * Process a bitmap to detect faces, crop to the first face, and draw eye contours.
     * 
     * @param bitmap The source bitmap to process
     * @param paddingPercent The padding percentage around the face (default: 20%)
     * @param eyePointSize The size of the eye contour points (default: 2f)
     * @param eyeContourColor The color of the eye contour points (default: RED)
     * @return A processed bitmap with the face cropped and eye contours drawn, or null if no face detected
     */
    suspend fun process(
        bitmap: Bitmap,
        paddingPercent: Float = DEFAULT_PADDING_PERCENT,
        eyePointSize: Float = DEFAULT_EYE_POINT_SIZE,
        eyeContourColor: Int = DEFAULT_EYE_CONTOUR_COLOR
    ): ProcessingResult = withContext(Dispatchers.IO) {
        try {
            // Detect faces
            val faces = detectFaces(bitmap)
            
            if (faces.isEmpty()) {
                return@withContext ProcessingResult.NoFaceDetected
            }
            
            // Use first face
            val face = faces[0]
            
            // Crop face
            val croppedBitmap = cropFace(bitmap, face, paddingPercent) ?: return@withContext ProcessingResult.Error("Failed to crop face")
            
            // Calculate crop offsets for accurate eye contour drawing
            val boundingBox = face.boundingBox
            val padding = (boundingBox.width() * paddingPercent).toInt()
            val offsetX = (boundingBox.left - padding).coerceAtLeast(0)
            val offsetY = (boundingBox.top - padding).coerceAtLeast(0)
            
            // Draw eye contours
            val resultBitmap = drawEyeContours(
                croppedBitmap, 
                face, 
                offsetX, 
                offsetY, 
                eyePointSize, 
                eyeContourColor
            )
            
            ProcessingResult.Success(
                originalBitmap = bitmap,
                detectedFaces = faces,
                croppedFaceBitmap = croppedBitmap,
                resultBitmap = resultBitmap
            )
        } catch (e: Exception) {
            ProcessingResult.Error("Error processing image: ${e.message}")
        }
    }
    
    /**
     * Detects faces in the provided bitmap
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
     * Crops a face from the bitmap with padding
     */
    fun cropFace(bitmap: Bitmap, face: Face, paddingPercent: Float = DEFAULT_PADDING_PERCENT): Bitmap? {
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
    fun drawEyeContours(
        bitmap: Bitmap,
        face: Face,
        offsetX: Int,
        offsetY: Int,
        pointSize: Float = DEFAULT_EYE_POINT_SIZE,
        contourColor: Int = DEFAULT_EYE_CONTOUR_COLOR
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = contourColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Draw left eye contour points
        face.getContour(FaceContour.LEFT_EYE)?.let { contour ->
            drawContourPoints(canvas, contour.points, offsetX, offsetY, paint, pointSize)
        }
        
        // Draw right eye contour points
        face.getContour(FaceContour.RIGHT_EYE)?.let { contour ->
            drawContourPoints(canvas, contour.points, offsetX, offsetY, paint, pointSize)
        }
        
        return mutableBitmap
    }
    
    /**
     * Helper method to draw contour points
     */
    private fun drawContourPoints(
        canvas: Canvas, 
        points: List<PointF>, 
        offsetX: Int, 
        offsetY: Int,
        paint: Paint,
        pointSize: Float
    ) {
        points.forEach { point ->
            val x = point.x - offsetX
            val y = point.y - offsetY
            canvas.drawCircle(x, y, pointSize, paint)
        }
    }
} 