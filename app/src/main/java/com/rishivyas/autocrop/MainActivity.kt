@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rishivyas.autocrop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.rishivyas.autocrop.data.StorageUtils
import com.rishivyas.autocrop.ui.CameraScreen
import com.rishivyas.autocrop.ui.components.HistorySheetContent
import com.rishivyas.autocrop.ui.theme.AutoCropTheme
import com.rishivyas.autocrop.utils.ImageUtils
import com.rishivyas.autocrop.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // ViewModel for managing state
    private val viewModel: MainViewModel by viewModels()
    
    // Activity result launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.CAMERA] == true -> {
                launchCamera()
            }
            else -> {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.photoUri?.let { uri ->
                try {
                    // Convert URI to Bitmap
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    // Process the image
                    viewModel.processBitmap(this, bitmap)
                    Toast.makeText(this, "Image captured successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val bitmap = ImageUtils.decodeUriToBitmap(this, uri)
                if (bitmap != null) {
                    viewModel.processBitmapFromGallery(this, bitmap)
                } else {
                    Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AutoCropApp(viewModel)
        }
    }
    
    /**
     * Checks for camera permission and launches camera if granted
     */
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        }
    }

    /**
     * Launches the camera intent
     */
    private fun launchCamera() {
        val photoFile = StorageUtils.createImageFile(this)
        photoFile?.let { file ->
            viewModel.photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            viewModel.currentPhotoPath = file.absolutePath
            
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, viewModel.photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            takePictureLauncher.launch(takePictureIntent)
        }
    }

    /**
     * Opens the gallery picker
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    /**
     * Main app composable
     */
    @Composable
    private fun AutoCropApp(viewModel: MainViewModel) {
        val showSheet by viewModel.showHistorySheet
        val sheetState = rememberModalBottomSheetState()
        val coroutineScope = rememberCoroutineScope()
        
        AutoCropTheme {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Box {
                    CameraScreen(
                        capturedImage = viewModel.capturedImage.value,
                        detectedFaces = viewModel.detectedFaces.value,
                        croppedFace = viewModel.croppedFace.value,
                        faceWithEyeContours = viewModel.faceWithEyeContours.value,
                        onCaptureClick = { 
                            viewModel.clearAllImageStates()
                            checkCameraPermissionAndLaunch() 
                        },
                        onCropClick = { viewModel.cropDetectedFace(this@MainActivity) },
                        onSaveClick = { viewModel.saveProcessedImage(this@MainActivity) },
                        onPickGalleryClick = { 
                            viewModel.clearAllImageStates()
                            openGallery() 
                        },
                        onClearCapturedImage = { viewModel.clearCapturedImageStates() },
                        onClearProcessedImage = { viewModel.clearProcessedImageStates() },
                        onViewHistoryClick = {
                            viewModel.loadHistoryImages(this@MainActivity)
                            viewModel.showHistorySheet.value = true
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    
                    if (showSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { viewModel.showHistorySheet.value = false },
                            sheetState = sheetState
                        ) {
                            HistorySheetContent(
                                images = viewModel.historyImages,
                                onClose = { 
                                    coroutineScope.launch { 
                                        sheetState.hide()
                                        viewModel.showHistorySheet.value = false 
                                    } 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    AutoCropTheme {
        CameraScreen(
            capturedImage = null,
            detectedFaces = emptyList(),
            croppedFace = null,
            faceWithEyeContours = null,
            onCaptureClick = {},
            onCropClick = {},
            onSaveClick = {},
            onPickGalleryClick = {},
            onClearCapturedImage = {},
            onClearProcessedImage = {},
            onViewHistoryClick = {}
        )
    }
} 