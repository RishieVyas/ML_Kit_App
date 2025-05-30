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

    private fun saveProcessedImage(bitmap: Bitmap) {
        try {
            val imagesDir = getExternalFilesDir("ProcessedFaces")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File(imagesDir, "face_$timestamp.jpg")

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            Toast.makeText(this, "Image saved to: ${imageFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoCropTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraScreen(
                        capturedImage = _capturedImage.value,
                        detectedFaces = _detectedFaces.value,
                        croppedFace = _croppedFace.value,
                        faceWithEyeContours = _faceWithEyeContours.value,
                        onCaptureClick = { checkCameraPermissionAndLaunch() },
                        onCropClick = { cropDetectedFace() },
                        onSaveClick = { _faceWithEyeContours.value?.let { saveProcessedImage(it) } },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCaptureClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Take Photo")
            }

            if (detectedFaces.isNotEmpty()) {
                Button(
                    onClick = onCropClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Crop Face")
                }
            }
        }

        // Original image with face detection
        capturedImage?.let { bitmap ->
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
                        text = "Original Image with Face Detection",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
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
            }
        }

        // Processed face with eye contours
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Processed Face with Eye Contours",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = onSaveClick) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = "Save Image"
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
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

        // Original cropped face (if no contours drawn)
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
                                .height(300.dp)
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
            onSaveClick = {}
        )
    }
}