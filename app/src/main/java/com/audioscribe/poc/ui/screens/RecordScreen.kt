package com.audioscribe.poc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audioscribe.poc.R
import com.audioscribe.poc.ui.theme.AppSurface
import com.audioscribe.poc.ui.viewmodel.AudioScribeViewModel
import kotlinx.coroutines.delay

@Composable
fun RecordScreen(
    viewModel: AudioScribeViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val isRecording by viewModel.isRecording.observeAsState(false)
    val recordingDuration by viewModel.recordingDuration.observeAsState(0L)
    val transcriptionState by viewModel.transcriptionState.observeAsState(AudioScribeViewModel.TranscriptionState.Idle)

    var lastRecordedPath by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AppSurface(modifier = Modifier.fillMaxSize()) {
        val currentState = transcriptionState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Record Audio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            when (currentState) {
                is AudioScribeViewModel.TranscriptionState.Processing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(24.dp))
                    Text(
                        text = currentState.progress,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
                is AudioScribeViewModel.TranscriptionState.Completed -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.padding(24.dp))
                    Text(
                        text = "Transcription Complete!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentState.transcript.title,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.padding(8.dp))
                            Text(
                                text = currentState.transcript.transcriptText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                is AudioScribeViewModel.TranscriptionState.Error -> {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.padding(24.dp))
                    Text(
                        text = "Transcription Failed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.padding(24.dp))
                    Button(
                        onClick = {
                            lastRecordedPath?.let { viewModel.transcribeRecording(it) }
                        }
                    ) {
                        Text("Retry Transcription")
                    }
                }
                is AudioScribeViewModel.TranscriptionState.Idle -> {
                    RecordingTimer(durationMs = recordingDuration, isRecording = isRecording)
                    Spacer(modifier = Modifier.padding(32.dp))
                    RecordingButton(
                        isRecording = isRecording,
                        onClick = {
                            if (isRecording) {
                                val result = viewModel.stopRecording()
                                if (result.isSuccess) {
                                    val path = result.getOrNull()
                                    if (path != null) {
                                        lastRecordedPath = path
                                        viewModel.transcribeRecording(path)
                                    }
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to stop recording"
                                }
                            } else {
                                val result = viewModel.startRecording()
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to start recording"
                                } else {
                                    errorMessage = null
                                }
                            }
                        }
                    )
                    
                    errorMessage?.let {
                        Spacer(modifier = Modifier.padding(24.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun RecordingTimer(durationMs: Long, isRecording: Boolean) {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    Text(
        text = timeString,
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun RecordingButton(isRecording: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(100.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Record",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
