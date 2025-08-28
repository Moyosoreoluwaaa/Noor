package com.noor.base_app_imageviewer_w_ocr_bg_scan

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noor.base_app_imageviewer_w_ocr.FullScreenImageScreen
import com.noor.base_app_imageviewer_w_ocr.presentation.screens.AlbumScreen
import java.net.URLDecoder
import java.net.URLEncoder

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