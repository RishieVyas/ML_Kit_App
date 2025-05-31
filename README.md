# AutoCrop: Face Detection and Processing Application

## 1. Project Overview

AutoCrop is an Android application that provides automated face detection, cropping, and eye contour drawing functionality using Google's ML Kit. The application enables users to capture photos using the device camera or select images from the gallery, then processes these images to detect faces, crop them, draw eye contours, and save the processed results.

### Key Features:
- **Camera Integration**: Capture photos directly from the device camera
- **Gallery Integration**: Select images from the device gallery
- **Face Detection**: Detect faces in images using Google ML Kit
- **Face Cropping**: Automatically crop detected faces
- **Eye Contour Drawing**: Draw contours around detected eyes
- **Image Saving**: Save processed images to device storage
- **History Viewing**: Browse previously processed images

## 2. Architecture

AutoCrop follows the MVVM (Model-View-ViewModel) architecture pattern, which provides a clean separation of concerns and enhances testability and maintainability. The application is structured into two main modules:

1. **App Module**: Contains the main application logic, UI components, and integration points
2. **FaceProcessor Module**: A standalone Android library (.aar) that encapsulates all face processing functionality

### Key Architectural Components:

- **UI Layer (View)**: Implemented with Jetpack Compose, providing a modern, declarative UI approach
- **ViewModel Layer**: Manages UI state and business logic, serving as a bridge between the UI and data layers
- **Data Layer**: Handles data operations, storage, and model classes
- **Utility Layer**: Contains helper classes for common operations
- **FaceProcessor Library**: Encapsulates ML Kit integration and face processing operations

## 3. Modules and Responsibilities

### FaceProcessor Library Module

The FaceProcessor library is a standalone Android library that encapsulates all face detection and processing functionality:

- **FaceProcessor.kt**: The main class responsible for:
  - Configuring and initializing the ML Kit Face Detector
  - Detecting faces in images
  - Cropping faces
  - Drawing eye contours on detected faces
  - Providing a unified processing pipeline

- **ProcessingResult.kt**: A sealed class representing the possible outcomes of face processing:
  - Success: Contains original, cropped, and result bitmaps
  - NoFaceDetected: Indicates no faces were found in the image
  - Error: Contains error information when processing fails

### App Module

The app module contains the main application logic and UI components:

- **MainActivity.kt**: The entry point of the application, responsible for:
  - Setting up the UI
  - Handling camera and gallery integration
  - Managing permission requests
  - Coordinating between UI and ViewModel

- **MainViewModel.kt**: Manages application state and business logic:
  - Tracks image processing states
  - Coordinates with the FaceProcessor library
  - Manages history and persistence

- **UI Components**:
  - **CameraScreen.kt**: Main screen displaying captured and processed images
  - **ActionButtons.kt**: UI component for various actions (camera, gallery, crop, etc.)
  - **ImageCard.kt**: Reusable component for displaying images
  - **HistorySheetContent.kt**: Bottom sheet displaying processing history

- **Data Classes**:
  - **SavedImage.kt**: Represents a saved image with metadata
  - **StorageUtils.kt**: Handles file operations for image saving and loading
  - **LoadSavedImages.kt**: Loads saved images with thumbnails

- **Utility Classes**:
  - **ImageUtils.kt**: Bridge between the app and the FaceProcessor library

## 4. Libraries and Tools Used

### Core Android Libraries:
- **Jetpack Compose**: Modern UI toolkit for building native Android UI
- **AndroidX**: Core Android components and extensions
- **Lifecycle Components**: ViewModel and LiveData for lifecycle-aware data handling
- **Kotlin Coroutines**: For asynchronous programming
- **Kotlin Flow**: For reactive programming

### ML and Image Processing:
- **Google ML Kit Face Detection**: For detecting faces and facial landmarks

### Image Handling:
- **Coil**: For efficient image loading and caching in Compose
- **Android Bitmap**: For image manipulation and processing

## 5. Permissions and Security

### Required Permissions:
- **CAMERA**: For capturing photos through the device camera
- **WRITE_EXTERNAL_STORAGE**: For saving processed images to device storage (only for Android 9 and below)

### Security Considerations:
- All image processing is performed locally on the device; no images are sent to external servers
- The ML Kit models are bundled with the app or downloaded automatically by Google Play Services
- Images are saved to the app's private storage directory, accessible only to the app itself
- The FileProvider implementation ensures secure file sharing between the app and system components
- No personally identifiable information is collected or stored

### Build the FaceProcessor AAR Library:
1. Open a terminal in the project directory
2. Run the following command:
   ```
   ./gradlew :faceprocessor:assembleRelease
   ```
3. The AAR file will be generated in `faceprocessor/build/outputs/aar/faceprocessor-release.aar`

## Security and Privacy Considerations

- **Local Processing**: All image and face processing occurs locally on the device, ensuring user privacy
- **Minimal Permissions**: The app requests only the minimum permissions necessary for functionality
- **Secure Storage**: Processed images are stored in the app's private directory
- **No Analytics**: The app does not collect user data or analytics
- **No Network Access**: The app does not require internet access for its core functionality

The AutoCrop application demonstrates how to leverage ML Kit's face detection capabilities while maintaining user privacy and security.
