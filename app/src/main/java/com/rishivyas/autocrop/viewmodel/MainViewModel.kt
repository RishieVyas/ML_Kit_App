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
import com.rishivyas.faceprocessor.ProcessingResult
import kotlinx.coroutines.launch
import java.io.File

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
                // Use the new unified processing method from our library
                when (val result = ImageUtils.processBitmap(bitmap)) {
                    is ProcessingResult.Success -> {
                        // Extract all the needed information from the result
                        detectedFaces.value = result.detectedFaces
                        croppedFace.value = result.croppedFaceBitmap
                        faceWithEyeContours.value = result.resultBitmap
                        Toast.makeText(context, "Face detected, cropped, and contours drawn from gallery image", Toast.LENGTH_SHORT).show()
                    }
                    is ProcessingResult.NoFaceDetected -> {
                        Toast.makeText(context, "No face detected in gallery image", Toast.LENGTH_SHORT).show()
                    }
                    is ProcessingResult.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Face detection failed for gallery image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Crops a detected face from the captured image
     */
    fun cropDetectedFace(context: Context) {
        val bitmap = capturedImage.value
        
        if (bitmap != null) {
            viewModelScope.launch {
                try {
                    // Use the new unified processing method from our library
                    when (val result = ImageUtils.processBitmap(bitmap)) {
                        is ProcessingResult.Success -> {
                            // Extract all the needed information from the result
                            detectedFaces.value = result.detectedFaces
                            croppedFace.value = result.croppedFaceBitmap
                            faceWithEyeContours.value = result.resultBitmap
                            Toast.makeText(context, "Face cropped and contours drawn", Toast.LENGTH_SHORT).show()
                        }
                        is ProcessingResult.NoFaceDetected -> {
                            Toast.makeText(context, "No face detected to crop", Toast.LENGTH_SHORT).show()
                        }
                        is ProcessingResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Face processing failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
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