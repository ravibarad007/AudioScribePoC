package com.audioscribe.poc

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.audioscribe.poc.ui.screens.HomeScreen
import com.audioscribe.poc.ui.screens.ModelSetupScreen
import com.audioscribe.poc.ui.screens.RecordScreen
import com.audioscribe.poc.ui.theme.AppTheme
import com.audioscribe.poc.ui.viewmodel.AudioScribeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AudioScribeViewModel by viewModels()

    private val audioPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importAudio(it) }
    }

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "model_setup") {
                        composable("model_setup") {
                            ModelSetupScreen(
                                viewModel = viewModel,
                                onModelReady = {
                                    navController.navigate("home") {
                                        popUpTo("model_setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onRecordClick = {
                                    if (checkMicPermission()) {
                                        navController.navigate("record")
                                    } else {
                                        requestMicPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onImportClick = { pickAudioFile() }
                            )
                        }
                        composable("record") {
                            RecordScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun pickAudioFile() {
        audioPicker.launch("audio/*")
    }
}
