// File: app/src/main/java/com/example/pedestrianai/ui/theme/HomeScreen.kt

package com.example.pedestrianai.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// --- Define Colors to Match the UI ---
val BrightBlue = Color(0xFF007BFF)
val TextBlack = Color(0xFF1C1C1E)
val TextGrey = Color(0xFF8A8A8E)
val BackgroundGrey = Color(0xFFF9F9F9)
val LightBlueBg = Color(0xFFE0F2FF)
val LightGreenBg = Color(0xFFE6F9E9)
val LightOrangeBg = Color(0xFFFFF4E0)


// Data class to hold information for each navigation card
data class DetectionMode(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val iconBgColor: Color,
    val iconColor: Color
)

@Composable
fun HomeScreen(navController: NavController) {
    // List of detection modes for cleaner code
    val detectionModes = listOf(
        DetectionMode(
            "Live Detection",
            "Use the camera for real-time analysis.",
            Icons.Outlined.CameraAlt,
            "live_detection",
            LightBlueBg,
            BrightBlue
        ),
        DetectionMode(
            "Image Detection",
            "Analyze a photo from your gallery.",
            Icons.Outlined.Image,
            "image_detection",
            LightGreenBg,
            Color(0xFF28A745)
        ),
        DetectionMode(
            "Video Detection",
            "Process a pre-recorded video file.",
            Icons.Outlined.Videocam,
            "video_detection",
            LightOrangeBg,
            Color(0xFFFD7E14)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGrey)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.5f)) // Pushes content towards the center

        // --- Main Title Section ---
        Text(
            text = "Pedestrian",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )
        Text(
            text = "Detection",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = BrightBlue,
            modifier = Modifier.padding(top = 0.dp, bottom = 24.dp)
        )

        // --- Subtitle Description ---
        Text(
            text = "Real-time AI-powered pedestrian detection using advanced computer vision. Select a mode below to get started.",
            fontSize = 16.sp,
            color = TextGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.weight(0.5f))

        // --- Clickable Navigation Cards ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            detectionModes.forEach { mode ->
                NavigationCard(
                    title = mode.title,
                    description = mode.description,
                    icon = mode.icon,
                    iconBackgroundColor = mode.iconBgColor,
                    iconColor = mode.iconColor,
                    onClick = { navController.navigate(mode.route) }
                )
            }
        }

        Spacer(Modifier.weight(1f)) // Pushes content up
    }
}


@Composable
fun NavigationCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make the whole card clickable
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // THIS IS THE FIX: The Column now fills the width of the card,
        // allowing its horizontalAlignment to properly center the content inside.
        Column(
            modifier = Modifier
                .fillMaxWidth() // <-- CRITICAL FIX
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextGrey,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}