package com.rishivyas.faceprocessor

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face

/**
 * Sealed class representing the possible outcomes of face processing.
 */
sealed class ProcessingResult {
    /**
     * Processing was successful, and we have results.
     */
    data class Success(
        val originalBitmap: Bitmap,
        val detectedFaces: List<Face>,
        val croppedFaceBitmap: Bitmap,
        val resultBitmap: Bitmap
    ) : ProcessingResult()
    
    /**
     * No face was detected in the image.
     */
    object NoFaceDetected : ProcessingResult()
    
    /**
     * An error occurred during processing.
     */
    data class Error(val message: String) : ProcessingResult()
} 