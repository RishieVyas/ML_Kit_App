package com.rishivyas.autocrop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.rishivyas.autocrop.ui.theme.AutoCropTheme
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import android.media.MediaScannerConnection
import androidx.compose.material.icons.filled.Close

class MainActivity : ComponentActivity() {
    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null
    private var originalBitmap: Bitmap? = null

    // State for the captured image, detected faces, and cropped face
    private val _capturedImage = mutableStateOf<Bitmap?>(null)
    private val _detectedFaces = mutableStateOf<List<Face>>(emptyList())
    private val _croppedFace = mutableStateOf<Bitmap?>(null)
    private val _faceWithEyeContours = mutableStateOf<Bitmap?>(null)

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
            photoUri?.let { uri ->
                try {
                    // Convert URI to Bitmap
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    // Store original bitmap and update state
                    originalBitmap = bitmap
                    _capturedImage.value = bitmap

                    // Process the image for face detection
                    detectFaces(bitmap)

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

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register gallery picker launcher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val bitmap = decodeUriToBitmap(uri)
                    if (bitmap != null) {
                        processBitmapFromGallery(bitmap)
                    } else {
                        Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setContent {
            AutoCropTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val noImages = _capturedImage.value == null && _croppedFace.value == null && _faceWithEyeContours.value == null
                    CameraScreen(
                        capturedImage = _capturedImage.value,
                        detectedFaces = _detectedFaces.value,
                        croppedFace = _croppedFace.value,
                        faceWithEyeContours = _faceWithEyeContours.value,
                        onCaptureClick = { clearAllImageStates(); checkCameraPermissionAndLaunch() },
                        onCropClick = { cropDetectedFace() },
                        onSaveClick = { _faceWithEyeContours.value?.let { saveProcessedImage(it) } },
                        onPickGalleryClick = { clearAllImageStates(); openGallery() },
                        onClearCapturedImage = { clearCapturedImageStates() },
                        onClearProcessedImage = { clearProcessedImageStates() },
                        onViewHistoryClick = { viewHistory() },
                        modifier = if (noImages) Modifier.fillMaxSize() else Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun clearAllImageStates() {
        _capturedImage.value = null
        _detectedFaces.value = emptyList()
        _croppedFace.value = null
        _faceWithEyeContours.value = null
    }

    private fun clearCapturedImageStates() {
        _capturedImage.value = null
        _detectedFaces.value = emptyList()
        _croppedFace.value = null
        _faceWithEyeContours.value = null
    }

    private fun clearProcessedImageStates() {
        _faceWithEyeContours.value = null
        _croppedFace.value = null
    }

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

    private fun launchCamera() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            takePictureLauncher.launch(takePictureIntent)
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("Pictures")
        return try {
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun decodeUriToBitmap(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun processBitmapFromGallery(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val boundingBox = face.boundingBox
                    val left = boundingBox.left.coerceIn(0, bitmap.width)
                    val top = boundingBox.top.coerceIn(0, bitmap.height)
                    val right = boundingBox.right.coerceIn(0, bitmap.width)
                    val bottom = boundingBox.bottom.coerceIn(0, bitmap.height)
                    val padding = (boundingBox.width() * 0.2f).toInt()
                    val paddedLeft = (left - padding).coerceIn(0, bitmap.width)
                    val paddedTop = (top - padding).coerceIn(0, bitmap.height)
                    val paddedRight = (right + padding).coerceIn(0, bitmap.width)
                    val paddedBottom = (bottom + padding).coerceIn(0, bitmap.height)
                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap,
                        paddedLeft,
                        paddedTop,
                        paddedRight - paddedLeft,
                        paddedBottom - paddedTop
                    )
                    _croppedFace.value = croppedBitmap
                    if (croppedBitmap != null) {
                        val faceWithContours = drawEyeContoursOnBitmap(croppedBitmap, face, paddedLeft, paddedTop)
                        _faceWithEyeContours.value = faceWithContours
                        Toast.makeText(this, "Face detected, cropped, and contours drawn from gallery image", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to crop face from gallery image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No face detected in gallery image", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Face detection failed for gallery image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun detectFaces(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                _detectedFaces.value = faces
                if (faces.isEmpty()) {
                    Toast.makeText(this, "No faces detected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "${faces.size} face(s) detected", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Face detection failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun adjustContourPoints(points: List<PointF>, boundingBox: Rect): List<PointF> {
        return points.map { point ->
            PointF(
                point.x - boundingBox.left,
                point.y - boundingBox.top
            )
        }
    }

    private fun drawEyeContoursOnBitmap(
        bitmap: Bitmap,
        face: Face,
        cropOffsetX: Int,
        cropOffsetY: Int
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = AndroidColor.RED
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

    private fun saveProcessedImage(bitmap: Bitmap) {
        try {
            val imagesDir = getExternalFilesDir("ProcessedFaces")
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
                this,
                arrayOf(imageFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropDetectedFace() {
        val bitmap = _capturedImage.value
        val faces = _detectedFaces.value

        if (bitmap != null && faces.isNotEmpty()) {
            val face = faces[0]
            val boundingBox = face.boundingBox

            // Ensure the bounding box is within the bitmap bounds
            val left = boundingBox.left.coerceIn(0, bitmap.width)
            val top = boundingBox.top.coerceIn(0, bitmap.height)
            val right = boundingBox.right.coerceIn(0, bitmap.width)
            val bottom = boundingBox.bottom.coerceIn(0, bitmap.height)

            // Add some padding around the face (20% of the face size)
            val padding = (boundingBox.width() * 0.2f).toInt()
            val paddedLeft = (left - padding).coerceIn(0, bitmap.width)
            val paddedTop = (top - padding).coerceIn(0, bitmap.height)
            val paddedRight = (right + padding).coerceIn(0, bitmap.width)
            val paddedBottom = (bottom + padding).coerceIn(0, bitmap.height)

            // Create the cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                paddedLeft,
                paddedTop,
                paddedRight - paddedLeft,
                paddedBottom - paddedTop
            )

            _croppedFace.value = croppedBitmap

            if (croppedBitmap != null) {
                // Draw eye contours on the cropped face with correct offset
                val faceWithContours = drawEyeContoursOnBitmap(croppedBitmap, face, paddedLeft, paddedTop)
                _faceWithEyeContours.value = faceWithContours
                Toast.makeText(this, "Face cropped and contours drawn", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to crop face", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No face detected to crop", Toast.LENGTH_SHORT).show()
        }
    }

    private fun viewHistory() {
        // Implementation of viewHistory function
        Toast.makeText(this, "View History function not implemented", Toast.LENGTH_SHORT).show()
    }
}

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
    onViewHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val noImages = capturedImage == null && croppedFace == null && faceWithEyeContours == null
    if (noImages) {
        // Center three buttons vertically in a column
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
        // Buttons at top, images below
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val showCrop = detectedFaces.isNotEmpty()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onCaptureClick,
                    modifier = Modifier.weight(1f)
                ) { Text("Take Photo") }
                if (showCrop) {
                    Button(
                        onClick = onCropClick,
                        modifier = Modifier.weight(1f)
                    ) { Text("Crop Face") }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onPickGalleryClick,
                    modifier = Modifier.weight(1f)
                ) { Text("Pick from Gallery") }
                Button(
                    onClick = onViewHistoryClick,
                    modifier = Modifier.weight(1f)
                ) { Text("View History") }
            }
            // Image cards below the grid
            capturedImage?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Original Image with Face Detection",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Captured Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )

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
            }
            faceWithEyeContours?.let { face ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
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
                                onClick = { onSaveClick() },
                                modifier = Modifier.align(Alignment.CenterStart).size(32.dp)
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
                                modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear Processed Image"
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .background(Color.White)
                        ) {
                            Image(
                                bitmap = face.asImageBitmap(),
                                contentDescription = "Processed Face",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
            if (faceWithEyeContours == null) {
                croppedFace?.let { face ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Cropped Face",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                Image(
                                    bitmap = face.asImageBitmap(),
                                    contentDescription = "Cropped Face",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
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