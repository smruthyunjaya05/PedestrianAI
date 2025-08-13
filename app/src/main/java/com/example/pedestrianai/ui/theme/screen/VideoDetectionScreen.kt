// File: app/src/main/java/com/example/pedestrianai/ui/theme/screen/VideoDetectionScreen.kt
package com.example.pedestrianai.ui.theme.screen

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pedestrianai.data.DetectionResult
import com.example.pedestrianai.ml.TFLiteDetector
import com.example.pedestrianai.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

private sealed class AnalysisState {
    object Idle : AnalysisState()
    data class Processing(val progress: Float) : AnalysisState()
    data class Complete(val framePaths: List<String>, val allDetections: List<List<DetectionResult>>, val aspectRatio: Float) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoDetectionScreen(navController: NavController) {
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> videoUri = uri }
    fun resetToUpload() { videoUri = null }
    val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    LaunchedEffect(Unit) {
        if (!readPermission.status.isGranted) {
            readPermission.launchPermissionRequest()
        }
    }
    Scaffold(
        containerColor = BackgroundGrey,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VideoScreenHeader(onBackClicked = {
                if (videoUri != null) resetToUpload() else navController.popBackStack()
            })
            Spacer(modifier = Modifier.height(16.dp))
            if (videoUri == null) {
                VideoUploadContent(onSelectClick = {
                    if (readPermission.status.isGranted) {
                        videoPickerLauncher.launch("video/*")
                    } else {
                        readPermission.launchPermissionRequest()
                    }
                })
            } else {
                VideoAnalysisManager(videoUri = videoUri!!, onAnalyzeAnother = { resetToUpload() })
            }
        }
    }
}

@Composable
private fun VideoAnalysisManager(videoUri: Uri, onAnalyzeAnother: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var analysisState by remember { mutableStateOf<AnalysisState>(AnalysisState.Idle) }
    val detector = remember {
        TFLiteDetector(context, "yolov8_pedestrian.tflite", null)
    }
    LaunchedEffect(videoUri) {
        analysisState = AnalysisState.Processing(0f)
        coroutineScope.launch(Dispatchers.IO) {
            val result = processAndSaveFrames(context, videoUri, detector) { progress ->
                launch(Dispatchers.Main) {
                    analysisState = AnalysisState.Processing(progress)
                }
            }
            withContext(Dispatchers.Main) {
                analysisState = result
            }
        }
    }
    DisposableEffect(Unit) { onDispose { detector.close() } }
    when (val state = analysisState) {
        is AnalysisState.Idle, is AnalysisState.Processing -> {
            ProcessingUI(progress = if (state is AnalysisState.Processing) state.progress else 0f)
        }
        is AnalysisState.Complete -> {
            PlaybackUI(
                framePaths = state.framePaths,
                allDetections = state.allDetections,
                aspectRatio = state.aspectRatio,
                onAnalyzeAnother = onAnalyzeAnother
            )
        }
        is AnalysisState.Error -> {
            ErrorUI(errorMessage = state.message, onAnalyzeAnother = onAnalyzeAnother)
        }
    }
}

private suspend fun processAndSaveFrames(
    context: Context,
    videoUri: Uri,
    detector: TFLiteDetector,
    onProgress: (Float) -> Unit
): AnalysisState {
    val TAG = "VideoAnalysis"
    val retriever = MediaMetadataRetriever()
    val outputDir = File(context.cacheDir, "frames_${System.currentTimeMillis()}").apply { mkdirs() }
    val framePaths = mutableListOf<String>()
    val allDetections = mutableListOf<List<DetectionResult>>()

    return try {
        Log.d(TAG, "Starting video processing for URI: $videoUri")
        retriever.setDataSource(context, videoUri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        if (durationMs <= 0L || width <= 0 || height <= 0) {
            return AnalysisState.Error("Invalid video properties (duration or dimensions are zero).")
        }
        val aspectRatio = width.toFloat() / height.toFloat()
        val intervalMs = 100L
        val frameCount = (durationMs / intervalMs).toInt()
        Log.d(TAG, "Total frames to process: $frameCount")
        for (i in 0..frameCount) {
            val timeUs = i * intervalMs * 1000L
            val originalBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (originalBitmap != null) {
                val modelWidth = 640
                val modelHeight = 640
                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, modelWidth, modelHeight, false)
                val modelInputBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                val results = detector.detect(modelInputBitmap, 0)
                // *** FIX: Removed the unnecessary 'i' parameter from the call ***
                val annotatedBitmap = drawBoundingBoxes(originalBitmap, results, modelWidth, modelHeight)
                val frameFile = File(outputDir, "frame_%04d.jpg".format(i))
                FileOutputStream(frameFile).use { out ->
                    annotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                framePaths.add(frameFile.absolutePath)
                allDetections.add(results)
                originalBitmap.recycle()
                resizedBitmap.recycle()
                modelInputBitmap.recycle()
                annotatedBitmap.recycle()
            }
            onProgress((i + 1).toFloat() / (frameCount + 1).toFloat())
        }
        AnalysisState.Complete(framePaths, allDetections, aspectRatio)
    } catch (e: Exception) {
        Log.e(TAG, "An exception occurred during video processing", e)
        outputDir.deleteRecursively()
        AnalysisState.Error(e.message ?: "An unknown error occurred.")
    } finally {
        retriever.release()
    }
}

@Composable
fun ProcessingUI(progress: Float) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = BrightBlue,
            strokeWidth = 8.dp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Text(text = "Analyzing ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextBlack)
            Text(text = "Video", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BrightBlue)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "This may take a moment...", style = MaterialTheme.typography.bodyLarge, color = TextGrey)
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(0.7f), color = BrightBlue, trackColor = LightBlueBg)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BrightBlue)
    }
}

@Composable
fun PlaybackUI(
    framePaths: List<String>,
    allDetections: List<List<DetectionResult>>,
    aspectRatio: Float,
    onAnalyzeAnother: () -> Unit
) {
    if (framePaths.isEmpty()) {
        ErrorUI(errorMessage = "No frames were processed from the video.", onAnalyzeAnother = onAnalyzeAnother)
        return
    }
    var currentFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var frameIndex by remember { mutableStateOf(0) }
    LaunchedEffect(framePaths) {
        while (isActive) {
            val oldBitmap = currentFrameBitmap
            val newBitmap = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(framePaths[frameIndex])
                } catch (e: Exception) {
                    Log.e("PlaybackUI", "Failed to load frame ${framePaths[frameIndex]}", e)
                    null
                }
            }
            currentFrameBitmap = newBitmap
            if (oldBitmap != null && oldBitmap != newBitmap) {
                oldBitmap.recycle()
            }
            frameIndex = (frameIndex + 1) % framePaths.size
            delay(100L)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            CoroutineScope(Dispatchers.IO).launch {
                File(framePaths.first()).parentFile?.deleteRecursively()
            }
        }
    }
    val currentResults = allDetections.getOrNull(frameIndex) ?: emptyList()
    val averageConfidence = if (currentResults.isNotEmpty()) currentResults.map { it.confidence }.average() * 100 else 0.0
    val decimalFormat = DecimalFormat("0.0'%'")
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetectionCompleteHeader()
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                currentFrameBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Analyzed Frame",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: CircularProgressIndicator()
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VideoResultStatCard(Modifier.weight(1f), Icons.Outlined.Groups, "Pedestrians Detected", currentResults.size.toString(), "Individuals identified.")
            VideoResultStatCard(Modifier.weight(1f), Icons.Outlined.Speed, "Avg. Confidence", if (currentResults.isNotEmpty()) decimalFormat.format(averageConfidence) else "N/A", "AI model confidence.")
        }
    }
}

@Composable
private fun DetectionCompleteHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Complete",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Detection Complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextBlack)
        }
        Text(text = "AI analysis has identified pedestrians.", style = MaterialTheme.typography.bodyLarge, color = TextGrey)
    }
}

@Composable
fun ErrorUI(errorMessage: String, onAnalyzeAnother: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Video analysis failed.", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Error: $errorMessage", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAnalyzeAnother) { Text("Try Another Video") }
    }
}

// *** FIX: Removed the unnecessary 'frameIndex' parameter from the function definition ***
private fun drawBoundingBoxes(bitmap: Bitmap, results: List<DetectionResult>, modelWidth: Int, modelHeight: Int): Bitmap {
    val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(outputBitmap)
    val scaleX = bitmap.width.toFloat() / modelWidth
    val scaleY = bitmap.height.toFloat() / modelHeight
    val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = BrightBlue.toArgb()
        strokeWidth = (bitmap.width / 150f).coerceIn(4f, 10f)
    }
    val textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = BrightBlue.copy(alpha = 0.7f).toArgb()
    }
    val textPaint = Paint().apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
        textSize = (bitmap.width / 40f).coerceIn(24f, 50f)
    }
    // `forEachIndexed` provides the correct index (0, 1, 2...) for pedestrians *within this single frame*
    results.forEachIndexed { index, result ->
        val b = result.boundingBox
        val scaledBox = RectF(b.left * scaleX, b.top * scaleY, b.right * scaleX, b.bottom * scaleY)
        canvas.drawRect(scaledBox, boxPaint)
        val tag = "Pedestrian ${index + 1}"
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(tag, 0, tag.length, textBounds)
        canvas.drawRect(scaledBox.left, scaledBox.top - textBounds.height() - 10, scaledBox.left + textBounds.width() + 20, scaledBox.top, textBackgroundPaint)
        canvas.drawText(tag, scaledBox.left + 10, scaledBox.top - 10, textPaint)
    }
    return outputBitmap
}

@Composable
private fun VideoUploadContent(onSelectClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Upload & Analyze Video", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextBlack)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Select a video file for frame-by-frame pedestrian detection.", style = MaterialTheme.typography.bodyLarge, color = TextGrey, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Box(modifier = Modifier.fillMaxWidth().background(LightBlueBg, RoundedCornerShape(16.dp)).border(width = 2.dp, color = BrightBlue, shape = RoundedCornerShape(16.dp)).padding(32.dp).clickable { onSelectClick() }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.UploadFile, contentDescription = "Upload", tint = BrightBlue, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Click to select a file", fontWeight = FontWeight.Bold, color = TextBlack)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Supports MP4, AVI, and more", color = TextGrey)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectClick, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BrightBlue), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Select Video", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun VideoResultStatCard(modifier: Modifier, icon: ImageVector, title: String, value: String, description: String) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(40.dp).background(LightBlueBg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = title, tint = BrightBlue)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextBlack)
            Text(title, color = TextGrey, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextGrey)
        }
    }
}

@Composable
private fun VideoScreenHeader(onBackClicked: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBackClicked) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextBlack)
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
            Text(text = "Video ", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextBlack)
            Text(text = "Analysis", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrightBlue)
        }
        Spacer(modifier = Modifier.size(48.dp))
    }
}