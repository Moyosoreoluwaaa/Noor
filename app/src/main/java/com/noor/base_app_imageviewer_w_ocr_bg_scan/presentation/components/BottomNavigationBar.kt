package com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.noor.base_app_note.Routes

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar {
//        NavigationBarItem(
//            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
//            label = { Text("Home") },
//            selected = currentRoute == Routes.HOME,
//            onClick = {
//                navController.navigate(Routes.HOME) {
//                    popUpTo(navController.graph.findStartDestination().id) {
//                        saveState = true
//                    }
//                    launchSingleTop = true
//                    restoreState = true
//                }
//            }
//        )

        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = "Notes") },
            label = { Text("Notes") },
            selected = currentRoute == Routes.NOTES_LIST,
            onClick = {
                navController.navigate(Routes.NOTES_LIST) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "OCR") },
            label = { Text("OCR") },
            selected = currentRoute == Routes.OCR_SCREEN,
            onClick = {
                navController.navigate(Routes.OCR_SCREEN) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

//        NavigationBarItem(
//            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
//            label = { Text("Settings") },
//            selected = currentRoute == Routes.SETTINGS,
//            onClick = {
//                navController.navigate(Routes.SETTINGS) {
//                    popUpTo(navController.graph.findStartDestination().id) {
//                        saveState = true
//                    }
//                    launchSingleTop = true
//                    restoreState = true
//                }
//            }
//        )
    }
}