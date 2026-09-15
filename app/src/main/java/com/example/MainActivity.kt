package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.TranslateScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: ScannerViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToCamera = { navController.navigate("camera") },
                                onNavigateToResult = { navController.navigate("result") },
                                onNavigateToChat = { navController.navigate("chat") },
                                onNavigateToAbout = { navController.navigate("about") }
                            )
                        }
                        composable("camera") {
                            CameraScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onCaptureSuccess = { navController.navigate("result") {
                                    popUpTo("home")
                                } }
                            )
                        }
                        composable("result") {
                            ResultScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToTranslate = { navController.navigate("translate") }
                            )
                        }
                        composable("translate") {
                            TranslateScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("chat") {
                            val contextText by viewModel.currentExtractedText.collectAsState()
                            LaunchedEffect(contextText) {
                                chatViewModel.setDocumentContext(contextText)
                            }
                            ChatScreen(
                                viewModel = chatViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("about") {
                            com.example.ui.screens.AboutScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
