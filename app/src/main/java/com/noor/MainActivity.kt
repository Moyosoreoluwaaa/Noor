package com.noor

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noor.base_app_1.data.AlbumScreen
import com.noor.base_app_1.data.FullScreenImageScreen
import com.noor.ui.theme.NoorTheme
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permission using Android 14 bottom sheet
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        setContent {
            NoorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoorApp()
                }
            }
        }
    }
}

// NoorApp.kt
@Composable
fun NoorApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "albums"
    ) {
        composable("albums") {
            AlbumScreen(
                onImageClick = { imageIndex, folderPath ->
                    val encodedPath = URLEncoder.encode(folderPath, "UTF-8")
                    navController.navigate("fullscreen/$imageIndex/$encodedPath")
                }
            )
        }
        composable(
            route = "fullscreen/{imageIndex}/{folderPath}",
            arguments = listOf(
                navArgument("imageIndex") { type = NavType.StringType },
                navArgument("folderPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageIndex = backStackEntry.arguments?.getString("imageIndex")?.toIntOrNull() ?: 0
            val encodedPath = backStackEntry.arguments?.getString("folderPath") ?: ""
            val folderPath = URLDecoder.decode(encodedPath, "UTF-8")

            FullScreenImageScreen(
                initialIndex = imageIndex,
                folderPath = folderPath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
