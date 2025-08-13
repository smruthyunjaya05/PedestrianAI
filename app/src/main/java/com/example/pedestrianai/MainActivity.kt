// File: app/src/main/java/com/example/pedestrianai/MainActivity.kt
package com.example.pedestrianai

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pedestrianai.ui.screen.ImageDetectionScreen
import com.example.pedestrianai.ui.screen.LiveDetectionScreen
import com.example.pedestrianai.ui.theme.HomeScreen
import com.example.pedestrianai.ui.theme.PedestrianAITheme
import com.example.pedestrianai.ui.theme.screen.VideoDetectionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView

            // Create a scale-up and fade-out animation on the icon view.
            val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.2f)
            val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.2f)
            val alpha = ObjectAnimator.ofFloat(iconView, View.ALPHA, 1f, 0f)

            val animationDuration = 500L
            scaleX.duration = animationDuration
            scaleY.duration = animationDuration
            alpha.duration = animationDuration

            scaleX.interpolator = AnticipateInterpolator()
            scaleY.interpolator = AnticipateInterpolator()

            alpha.doOnEnd {
                splashScreenView.remove()
            }

            scaleX.start()
            scaleY.start()
            alpha.start()
        }

        setContent {
            PedestrianAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("live_detection") {
                            LiveDetectionScreen(navController = navController)
                        }
                        composable("image_detection") {
                            ImageDetectionScreen(navController = navController)
                        }
                        composable("video_detection") {
                            VideoDetectionScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}