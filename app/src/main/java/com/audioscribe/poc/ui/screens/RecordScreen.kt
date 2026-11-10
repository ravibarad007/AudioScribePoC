package com.audioscribe.poc.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Surface
import com.audioscribe.poc.ui.theme.AppSurface
import com.audioscribe.poc.ui.theme.AppTheme
import com.audioscribe.poc.ui.viewmodel.AudioScribeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.launch

@Composable
fun RecordScreen(
    viewModel: AudioScribeViewModel = viewModel(),
    onBack: () -> Unit,
    onTranscriptSaved: (String) -> Unit
) {
    val isRecording by viewModel.isRecording.observeAsState()
    val recordingDuration by viewModel.recordingDuration.observeAsState()
    val transcriptionState by viewModel.transcriptionState.observeAsState()
    
    val showTranscript by remember { mutableStateOf(false) }
    val transcriptText by remember { mutableStateOf("") }
    
    // Handle transcription completion
    androidx.compose.runtime.LaunchedEffect(transcriptionState) {
        when (transcriptionState) {
            is AudioScribeViewModel.TranscriptionState.Completed -> {
                transcriptText = transcriptionState.transcript.transcriptText
                showTranscript = true
            }
            is AudioScribeViewModel.TranscriptionState.Error -> {
                // Error handled in UI
            }
            else -> {}
        }
    }
    
    AppSurface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = stringResource(id = com.audioscribe.poc.R.string.record_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Spacer for balance
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.width(48.dp))
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(32.dp))
            
            if (showTranscript) {
                // Show transcript result
                TranscriptResultCard(
                    transcriptText = transcriptText,
                    onSave = {
                        onTranscriptSaved(transcriptText)
                        showTranscript = false
                    },
                    onDiscard = {
                        showTranscript = false
                        // Reset transcription state
                    },
                    onCopy = {
                        android.content.ClipboardManager.getSystemService(
                            android.content.Context.CLIPBOARD_SERVICE
                        )?.setPrimaryClip(
                            android.content.ClipData.newPlainText("transcript", transcriptText)
                        )
                    }
                )
            } else {
                // Recording interface
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Recording visualization
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer pulse ring when recording
                        if (isRecording) {
                            RecordingPulseRing()
                        }
                        
                        // Main record button
                        Button(
                            onClick = {
                                if (isRecording) {
                                    val result = viewModel.stopRecording()
                                    if (result.isSuccess) {
                                        viewModel.transcribeRecording(result.getOrThrow())
                                    }
                                } else {
                                    viewModel.startRecording()
                                }
                            },
                            modifier = Modifier.size(140.dp),
                            shape = RoundedCornerShape(70.dp),
                            colors = if (isRecording) {
                                androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            } else {
                                androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(24.dp))
                    
                    // Duration display
                    Text(
                        text = formatDuration(recordingDuration),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(8.dp))
                    
                    Text(
                        text = if (isRecording) "Recording..." else "Tap to start recording",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(32.dp))
                    
                    // Transcription state
                    when (transcriptionState) {
                        is AudioScribeViewModel.TranscriptionState.Processing -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 3.dp
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))
                                    Text(
                                        text = transcriptionState.progress,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        is AudioScribeViewModel.TranscriptionState.Error -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(16.dp))
                                    Text(
                                        text = transcriptionState.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptResultCard(
    transcriptText: String,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(300.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Transcription Result",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row {
                        IconButton(onClick = onCopy) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy transcript",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(8.dp))
                
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = transcriptText,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 15,
                            overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(text = stringResource(id = com.audioscribe.poc.R.string.record_discard))
                }
                
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = com.audioscribe.poc.R.string.record_save))
                }
            }
        }
    }
}

@Composable
fun RecordingPulseRing() {
    // Animated pulsing ring - simplified for PoC
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animationSpec = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    
    androidx.compose.ui.graphics.drawscope.drawCircle(
        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        center = androidx.compose.ui.geometry.Offset(100.dp.toPx(), 100.dp.toPx()),
        radius = 100.dp.toPx() * scale
    )
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun stringResource(id: Int): String = androidx.compose.ui.res.stringResource(id)
