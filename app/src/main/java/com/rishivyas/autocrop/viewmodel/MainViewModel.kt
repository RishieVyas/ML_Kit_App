package com.rishivyas.autocrop.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.face.Face
import com.rishivyas.autocrop.data.LoadSavedImages
import com.rishivyas.autocrop.data.SavedImage
import com.rishivyas.autocrop.data.StorageUtils
import com.rishivyas.autocrop.utils.ImageUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for managing image processing and state
 */
class MainViewModel : ViewModel() {
    
    // Image processing states
    val capturedImage = mutableStateOf<Bitmap?>(null)
    val detectedFaces = mutableStateOf<List<Face>>(emptyList())
    val croppedFace = mutableStateOf<Bitmap?>(null)
    val faceWithEyeContours = mutableStateOf<Bitmap?>(null)
    
    // Bottom sheet states
    val showHistorySheet = mutableStateOf(false)
    val historyImages = mutableStateListOf<SavedImage>()
    
    // Other state
    var currentPhotoPath: String? = null
    var photoUri: Uri? = null
    
    /**
     * Clears all image states
     */
    fun clearAllImageStates() {
        capturedImage.value = null
        detectedFaces.value = emptyList()
        croppedFace.value = null
        faceWithEyeContours.value = null
    }
    
    /**
     * Clears only the captured image states
     */
    fun clearCapturedImageStates() {
        capturedImage.value = null
        detectedFaces.value = emptyList()
        croppedFace.value = null
        faceWithEyeContours.value = null
    }
    
    /**
     * Clears only the processed image states
     */
    fun clearProcessedImageStates() {
        faceWithEyeContours.value = null
        croppedFace.value = null
    }
    
    /**
     * Processes a bitmap from camera or gallery
     */
    fun processBitmap(context: Context, bitmap: Bitmap) {
        capturedImage.value = bitmap
        viewModelScope.launch {
            try {
                val faces = ImageUtils.detectFaces(bitmap)
                detectedFaces.value = faces
                
                if (faces.isEmpty()) {
                    Toast.makeText(context, "No faces detected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "${faces.size} face(s) detected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Face detection failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Processes a bitmap from gallery
     */
    fun processBitmapFromGallery(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val faces = ImageUtils.detectFaces(bitmap)
                
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val boundingBox = face.boundingBox
                    
                    // Calculate crop area
                    val left = boundingBox.left.coerceIn(0, bitmap.width)
                    val top = boundingBox.top.coerceIn(0, bitmap.height)
                    val padding = (boundingBox.width() * 0.2f).toInt()
                    val paddedLeft = (left - padding).coerceIn(0, bitmap.width)
                    val paddedTop = (top - padding).coerceIn(0, bitmap.height)
                    
                    // Crop the face
                    val croppedBitmap = ImageUtils.cropFace(bitmap, face)
                    croppedFace.value = croppedBitmap
                    
                    // Draw contours if we have a cropped face
                    if (croppedBitmap != null) {
                        val faceWithContours = ImageUtils.drawEyeContoursOnBitmap(
                            croppedBitmap, face, paddedLeft, paddedTop
                        )
                        faceWithEyeContours.value = faceWithContours
                        Toast.makeText(context, "Face detected, cropped, and contours drawn from gallery image", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to crop face from gallery image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "No face detected in gallery image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Face detection failed for gallery image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Crops a detected face from the captured image
     */
    fun cropDetectedFace(context: Context) {
        val bitmap = capturedImage.value
        val faces = detectedFaces.value
        
        if (bitmap != null && faces.isNotEmpty()) {
            val face = faces[0]
            val boundingBox = face.boundingBox
            
            // Calculate crop area
            val left = boundingBox.left.coerceIn(0, bitmap.width)
            val top = boundingBox.top.coerceIn(0, bitmap.height)
            val padding = (boundingBox.width() * 0.2f).toInt()
            val paddedLeft = (left - padding).coerceIn(0, bitmap.width)
            val paddedTop = (top - padding).coerceIn(0, bitmap.height)
            
            // Crop the face
            val croppedBitmap = ImageUtils.cropFace(bitmap, face)
            croppedFace.value = croppedBitmap
            
            // Draw contours if we have a cropped face
            if (croppedBitmap != null) {
                val faceWithContours = ImageUtils.drawEyeContoursOnBitmap(
                    croppedBitmap, face, paddedLeft, paddedTop
                )
                faceWithEyeContours.value = faceWithContours
                Toast.makeText(context, "Face cropped and contours drawn", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to crop face", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No face detected to crop", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Saves the processed image
     */
    fun saveProcessedImage(context: Context) {
        faceWithEyeContours.value?.let { bitmap ->
            StorageUtils.saveProcessedImage(context, bitmap)
        }
    }
    
    /**
     * Loads saved images
     */
    fun loadHistoryImages(context: Context) {
        viewModelScope.launch {
            historyImages.clear()
            historyImages.addAll(LoadSavedImages.loadSavedImagesWithThumbnails(context))
        }
    }
} 