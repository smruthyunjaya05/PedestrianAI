# 🚶 PedestrianAI: Real-Time Pedestrian Detection on Android

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6.8-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow-Lite-FF6F00.svg)](https://www.tensorflow.org/lite)
[![CameraX](https://img.shields.io/badge/CameraX-1.3.1-3DDC84.svg)](https://developer.android.com/training/camerax)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Harnessing the power of on-device AI to identify pedestrians in real-time, from images, and in videos.** 🚀

---

<!-- This HTML table creates the side-by-side layout for the logo and overview -->
<table>
  <tr>
    <td valign="top" width="200">
      <!-- *** FIX: Changed the image source to the new rounded logo *** -->
      <img src="app/src/main/assets/app_logo_rounded.png" alt="PedestrianAI Logo" width="180"/>
    </td>
    <td valign="top">
      <h2>🎯 Overview</h2>
      <p><strong>PedestrianAI</strong> is a modern, native Android application built with Kotlin and Jetpack Compose that demonstrates the power of on-device machine learning for computer vision tasks. The app provides a suite of tools for detecting pedestrians through three distinct modes: a live camera feed, static image analysis, and pre-recorded video processing.</p>
      <p>Powered by a <strong>YOLOv8</strong> model converted to TensorFlow Lite, the application is designed to be efficient, responsive, and provide a seamless user experience. It showcases advanced Android development concepts including CameraX integration, modern UI with Compose, and complex background processing for video analysis.</p>
    </td>
  </tr>
</table>

### ✨ Key Highlights
- 🧠 **On-Device Inference**: All AI processing happens locally for privacy and speed.
- 📸 **Live Camera Detection**: Real-time bounding boxes overlaid on a live camera feed.
- 🖼️ **Image Analysis**: Upload any image to detect and highlight all pedestrians.
- 🎞️ **Robust Video Processing**: A stable, memory-safe pipeline that processes videos frame-by-frame, draws bounding boxes, and presents a final, playable result.
- 🎨 **Modern, Responsive UI**: Built entirely with Jetpack Compose, following Material Design principles.
- 🚀 **High Performance**: Optimized for efficiency with coroutines for background tasks and a custom TFLite wrapper.

---

## 🖥️ Live Demo & Screenshots

<div align="center">
  <img src="app/src/main/assets/home_screen.png" alt="Home Screen" width="80%"/>
  <br/>
  <sub><strong>🏠 Home Screen:</strong> A clean, modern entry point offering three distinct detection modes: Live, Image, and Video analysis.</sub>
  <br/><br/>
  <img src="app/src/main/assets/live_detection.png" alt="Live Camera Detection" width="80%"/>
  <br/>
  <sub><strong>📸 Live Camera Detection:</strong> An interactive, real-time camera view that draws bounding boxes around detected pedestrians, complete with inference time stats.</sub>
  <br/><br/>
  <img src="app/src/main/assets/image_results.png" alt="Image Analysis Results" width="80%"/>
  <br/>
  <sub><strong>🖼️ Image Analysis:</strong> Upload an image to receive a detailed breakdown, including numbered bounding boxes, total pedestrian count, and a list of individual confidence scores.</sub>
  <br/><br/>
  <img src="app/src/main/assets/video_processing.png" alt="Video Processing UI" width="80%"/>
  <br/>
  <sub><strong>⏳ Video Processing:</strong> A clear, themed progress screen informs the user while the robust background pipeline analyzes the video frame by frame.</sub>
  <br/><br/>
  <img src="app/src/main/assets/video_results.png" alt="Video Analysis Results" width="80%"/>
  <br/>
  <sub><strong>🎞️ Video Results:</strong> The memory-safe playback screen shows the final video with all bounding boxes "baked in," along with frame-by-frame detection statistics.</sub>
</div>

---

## 🚀 Core Features

### 📸 CameraX Live Detection
- **Real-Time Overlay**: A custom `OverlayView` draws bounding boxes directly on top of the camera preview.
- **Front/Back Camera Switching**: Seamlessly switch between camera lenses during live detection.
- **Performance Metrics**: Live display of model inference time in milliseconds.
- **Permission Handling**: A clean, user-friendly screen for requesting camera permissions.

### 🖼️ Static Image Analysis
- **Image Upload**: Select any image from the device's gallery.
- **Detailed Results**: After processing, the UI displays the annotated image, total pedestrian count, average confidence, and a detailed list of scores.
- **Numbered Bounding Boxes**: Each detected pedestrian is individually numbered for clarity.

### 🎞️ "Process-and-Save" Video Pipeline
- **Stable & Crash-Proof**: A memory-safe architecture that processes videos by saving annotated frames to disk, preventing `OutOfMemoryError` crashes.
- **Frame-by-Frame Analysis**: Uses `MediaMetadataRetriever` to reliably extract frames from a video file.
- **UI Feedback**: A dynamic progress screen keeps the user informed during the analysis phase.
- **Animated Playback**: After processing is complete, the app plays back the sequence of saved JPEGs, creating a smooth video result.

---

## 🛠️ Getting Started

### 📋 Prerequisites
- **Android Studio** (Hedgehog or newer recommended)
- **Android SDK** (API Level 34 recommended)
- **An Android device or emulator** with API Level 26 or higher

### ⚡ Quick Start

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd PedestrianAI

---

## 🛠️ Getting Started

### 📋 Prerequisites
- **Android Studio** (Hedgehog or newer recommended)
- **Android SDK** (API Level 34 recommended)
- **An Android device or emulator** with API Level 26 or higher

### ⚡ Quick Start

1.  **Clone the repository**
    ```bash
    git clone <your-repo-url>
    cd PedestrianAI
    ```

2.  **Open the project in Android Studio**
    - From the Android Studio welcome screen, select "Open" and navigate to the cloned `PedestrianAI` directory.
    - Wait for Gradle to sync the project dependencies.

3.  **Place the TFLite Model**
    - You must have a TensorFlow Lite model file.
    - Rename your model to **`yolov8_pedestrian.tflite`**.
    - In the Android Studio Project panel, navigate to `app > src > main` and create a new directory named `assets`.
    - Place your `.tflite` file inside this `assets` folder.

4.  **Build and Run the application**
    - Select your target device from the dropdown menu.
    - Click the "Run 'app'" button (the green play icon).

---

## 🛡️ Technologies

| Technology | Purpose |
|------------|---------|
| **Kotlin** | Primary programming language for modern Android development. |
| **Jetpack Compose** | The modern, declarative UI toolkit for building native Android UI. |
| **TensorFlow Lite** | On-device machine learning framework for running the YOLOv8 model. |
| **CameraX** | A Jetpack library for building robust, high-performance camera functionality. |
| **Coroutines** | For managing background threads and asynchronous tasks, especially video processing. |
| **Material 3** | The latest version of Google's design system for theming and components. |
| **Accompanist**| A group of libraries for handling permissions in Jetpack Compose. |

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1.  **Fork the Project**
2.  **Create your Feature Branch** (`git checkout -b feature/AmazingFeature`)
3.  **Commit your Changes** (`git commit -m 'Add some AmazingFeature'`)
4.  **Push to the Branch** (`git push origin feature/AmazingFeature`)
5.  **Open a Pull Request**

### 📝 Development Guidelines
- Follow the official [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html).
- Write meaningful commit messages.
- Ensure the app builds and runs without errors before submitting.

---

<div align="center">
  <strong>🧠 Fast • Private • On-Device AI</strong>
  <br/>
  <em>Built with ❤️ and the latest Android technologies</em>
</div>