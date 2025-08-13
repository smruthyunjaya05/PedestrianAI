package com.example.pedestrianai.ui.screen

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.pedestrianai.data.DetectionResult
import com.example.pedestrianai.ml.TFLiteDetector
import com.example.pedestrianai.ui.theme.BackgroundGrey
import com.example.pedestrianai.ui.theme.BrightBlue
import com.example.pedestrianai.ui.theme.TextBlack
import com.example.pedestrianai.ui.theme.TextGrey
import com.example.pedestrianai.ui.view.OverlayView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LiveDetectionScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    var detectionResults by remember { mutableStateOf<List<DetectionResult>>(emptyList()) }
    var inferenceTime by remember { mutableStateOf(0L) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    val detector = remember {
        TFLiteDetector(
            context = context,
            modelPath = "yolov8_pedestrian.tflite",
            detectorListener = object : TFLiteDetector.DetectorListener {
                override fun onError(error: String) {
                    Log.e("LiveDetectionScreen", "Detector Error: $error")
                }

                override fun onResults(
                    results: List<DetectionResult>?,
                    infTime: Long,
                    imgHeight: Int,
                    imgWidth: Int
                ) {
                    detectionResults = results ?: emptyList()
                    inferenceTime = infTime
                    imageHeight = imgHeight
                    imageWidth = imgWidth
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
        }
    }

    Scaffold(
        containerColor = BackgroundGrey
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (cameraPermissionState.status.isGranted) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScreenHeader(
                        titlePart1 = "Live ",
                        titlePart2 = "Detection",
                        onBackClicked = { navController.popBackStack() },
                        showSwitchCameraButton = true,
                        onSwitchCameraClicked = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black)
                    ) {
                        CameraView(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            detector = detector,
                            lensFacing = lensFacing
                        )
                        AndroidView(
                            factory = { ctx -> OverlayView(ctx, null) },
                            modifier = Modifier.fillMaxSize(),
                            update = { overlayView ->
                                overlayView.setResults(
                                    detectionResults,
                                    imageHeight,
                                    imageWidth
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DetectionStats(
                        pedestrianCount = detectionResults.size,
                        inferenceTime = inferenceTime
                    )
                }
            } else {
                PermissionRequestScreen {
                    cameraPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(
    titlePart1: String,
    titlePart2: String,
    onBackClicked: () -> Unit,
    showSwitchCameraButton: Boolean = false,
    onSwitchCameraClicked: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClicked) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextBlack)
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = titlePart1,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            Text(
                text = titlePart2,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BrightBlue
            )
        }

        if (showSwitchCameraButton) {
            IconButton(onClick = onSwitchCameraClicked) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = TextBlack
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

// --- THIS IS THE CORRECTED COMPOSABLE ---
@Composable
fun CameraView(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    detector: TFLiteDetector,
    lensFacing: Int
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Use the `key` composable. When `lensFacing` changes, everything inside this block,
    // including the AndroidView, will be recomposed. This forces the camera to re-initialize
    // with the new lens setting.
    key(lensFacing) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            if (bitmap != null) {
                                detector.detect(bitmap, imageProxy.imageInfo.rotationDegrees)
                            }
                            imageProxy.close()
                        }
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()
                    // Bind the use cases to the camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("CameraView", "Use case binding failed", exc)
                }
                previewView
            }
        )
    }
}

@Composable
fun DetectionStats(pedestrianCount: Int, inferenceTime: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(label = "Pedestrians", value = "$pedestrianCount")
            StatItem(label = "Inference Time", value = "${inferenceTime}ms")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextGrey, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = BrightBlue,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = "Camera Icon",
            modifier = Modifier.size(80.dp),
            tint = TextGrey
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Permission Required",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This feature needs access to your camera to perform live object detection. Please grant the permission to continue.",
            color = TextGrey,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrightBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Grant Permission", color = Color.White, fontSize = 16.sp)
        }
    }
}