package com.rishivyas.autocrop.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.face.Face
import com.rishivyas.autocrop.ui.components.ActionButtons
import com.rishivyas.autocrop.ui.components.ImageCard

/**
 * Main screen for the camera functionality
 */
@Composable
fun CameraScreen(
    capturedImage: Bitmap?,
    detectedFaces: List<Face>,
    croppedFace: Bitmap?,
    faceWithEyeContours: Bitmap?,
    onCaptureClick: () -> Unit,
    onCropClick: () -> Unit,
    onSaveClick: () -> Unit,
    onPickGalleryClick: () -> Unit,
    onClearCapturedImage: () -> Unit,
    onClearProcessedImage: () -> Unit,
    modifier: Modifier = Modifier,
    onViewHistoryClick: () -> Unit = {}
) {
    val noImages = capturedImage == null && croppedFace == null && faceWithEyeContours == null
    
    if (noImages) {
        // Empty state - show only buttons centered
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onCaptureClick,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) { Text("Take Photo") }
                
                Button(
                    onClick = onPickGalleryClick,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) { Text("Pick from Gallery") }
                
                Button(
                    onClick = onViewHistoryClick,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) { Text("View History") }
            }
        }
    } else {
        // Show images and action buttons
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Action buttons at the top
            ActionButtons(
                onCaptureClick = onCaptureClick,
                onPickGalleryClick = onPickGalleryClick,
                onCropClick = onCropClick,
                onViewHistoryClick = onViewHistoryClick,
                showCrop = detectedFaces.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Display captured image with face detection overlay
            capturedImage?.let { bitmap ->
                Box {
                    ImageCard(
                        title = "Original Image with Face Detection",
                        image = bitmap.asImageBitmap(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Face detection overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val scaleX = size.width / bitmap.width.toFloat()
                            val scaleY = size.height / bitmap.height.toFloat()
                            
                            detectedFaces.forEach { face ->
                                // Draw bounding box
                                val boundingBox = face.boundingBox
                                drawRect(
                                    color = Color.Green,
                                    topLeft = Offset(
                                        boundingBox.left * scaleX,
                                        boundingBox.top * scaleY
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        boundingBox.width() * scaleX,
                                        boundingBox.height() * scaleY
                                    ),
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = onClearCapturedImage,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear Image"
                        )
                    }
                }
            }
            
            // Display processed face with eye contours
            faceWithEyeContours?.let { face ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            IconButton(
                                onClick = onSaveClick,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = "Save Image"
                                )
                            }
                            
                            Text(
                                text = "Cropped Image",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Center),
                                maxLines = 1
                            )
                            
                            IconButton(
                                onClick = onClearProcessedImage,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear Processed Image"
                                )
                            }
                        }
                        
                        Image(
                            bitmap = face.asImageBitmap(),
                            contentDescription = "Processed Face",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            // Show cropped face (without contours) if available but contours not drawn yet
            if (faceWithEyeContours == null) {
                croppedFace?.let { face ->
                    ImageCard(
                        title = "Cropped Face",
                        image = face.asImageBitmap(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
} 