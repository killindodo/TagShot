package com.tagshot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tagshot.app.ui.MainScreen
import com.tagshot.app.ui.PhotoDetailScreen
import com.tagshot.app.ui.PhotoViewModel
import com.tagshot.app.ui.TagManagerScreen
import com.tagshot.app.ui.theme.TagShotTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PhotoViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val handle = extras.createSavedStateHandle()
                val app = application as TagShotApp
                return PhotoViewModel(app.container.photoRepository, handle) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeModeFlow.collectAsState()
            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            TagShotTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { photoId ->
                                    navController.navigate("detail/$photoId")
                                },
                                onNavigateToTagManager = {
                                    navController.navigate("tag_manager")
                                }
                            )
                        }

                        composable(
                            route = "detail/{photoId}",
                            arguments = listOf(
                                navArgument("photoId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
                            PhotoDetailScreen(
                                initialPhotoId = photoId,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    if (navController.currentDestination?.route?.startsWith("detail") == true) {
                                        navController.popBackStack("main", inclusive = false)
                                    }
                                }
                            )
                        }

                        composable("tag_manager") {
                            TagManagerScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    if (navController.currentDestination?.route == "tag_manager") {
                                        navController.popBackStack("main", inclusive = false)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
