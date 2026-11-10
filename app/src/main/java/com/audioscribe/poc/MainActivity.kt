package com.audioscribe.poc

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.audioscribe.poc.ui.screens.HomeScreen
import com.audioscribe.poc.ui.screens.ModelSetupScreen
import com.audioscribe.poc.ui.screens.RecordScreen
import com.audioscribe.poc.ui.theme.AppTheme
import com.audioscribe.poc.ui.viewmodel.AudioScribeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val viewModel: AudioScribeViewModel by viewModels()
    
    // File picker for audio import
    private val audioPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importAudio(it)
        }
    }
    
    // Microphone permission request
    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted, recording can start
        }
    }
    
    // Storage permission request (for older APIs)
    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Handle storage permission result
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
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
                                onImportClick = {
                                    pickAudioFile()
                                }
                            )
                        }
                        
                        composable("record") {
                            RecordScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onTranscriptSaved = { transcript ->
                                    navController.popBackStack()
                                    // Transcript saved, home screen will update automatically
                                }
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
        // Use audio/* MIME type for audio files
        audioPicker.launch("audio/*")
    }
    
    // Handle permission rationale if needed
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Permission results handled by ActivityResultContracts
    }
}
