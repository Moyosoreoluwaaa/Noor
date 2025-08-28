package com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.BottomNavigationBar
import com.noor.base_app_imageviewer_w_ocr_bg_scan.presentation.components.NavigationCard
import com.noor.base_app_note.Routes

@Composable
fun HomeScreen(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Note App",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Navigation Cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    NavigationCard(
                        title = "My Notes",
                        subtitle = "View all notes",
                        icon = Icons.AutoMirrored.Filled.Note,
                        onClick = { navController.navigate(Routes.NOTES_LIST) }
                    )
                }

                item {
                    NavigationCard(
                        title = "OCR Scanner",
                        subtitle = "Process screenshots",
                        icon = Icons.Default.PhotoCamera,
                        onClick = { navController.navigate(Routes.OCR_SCREEN) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                item {
                    NavigationCard(
                        title = "Settings",
                        subtitle = "App preferences",
                        icon = Icons.Default.Settings,
                        onClick = { navController.navigate(Routes.SETTINGS) }
                    )
                }

                item {
                    NavigationCard(
                        title = "Statistics",
                        subtitle = "Usage stats",
                        icon = Icons.Default.BarChart,
                        onClick = { /* Navigate to stats */ }
                    )
                }
            }
        }
    }
}
