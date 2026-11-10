package com.audioscribe.poc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audioscribe.poc.R
import com.audioscribe.poc.data.download.DownloadState
import com.audioscribe.poc.ui.theme.AppSurface
import com.audioscribe.poc.ui.viewmodel.AudioScribeViewModel

@Composable
fun ModelSetupScreen(
    viewModel: AudioScribeViewModel = viewModel(),
    onModelReady: () -> Unit
) {
    val downloadState by viewModel.downloadState.observeAsState(DownloadState.Idle)
    val isModelReady by viewModel.isModelReady.observeAsState(false)

    LaunchedEffect(isModelReady) {
        if (isModelReady) {
            onModelReady()
        }
    }

    AppSurface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val currentState = downloadState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                when (currentState) {
                    is DownloadState.Downloading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(120.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    is DownloadState.Completed -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Download complete",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    is DownloadState.Error -> {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Download error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Model info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(24.dp))

            Text(
                text = stringResource(id = R.string.model_download_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = stringResource(id = R.string.model_download_subtitle),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.padding(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentState) {
                        is DownloadState.Idle -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.padding(8.dp))
                                Text(
                                    text = stringResource(id = R.string.model_download_size),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.padding(16.dp))
                            Text(
                                text = stringResource(id = R.string.model_download_note),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.padding(24.dp))
                            Button(
                                onClick = { viewModel.downloadModel() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Download Model", fontWeight = FontWeight.Medium)
                            }
                        }

                        is DownloadState.Downloading -> {
                            val progress = currentState.progress
                            val downloaded = currentState.bytesDownloaded
                            val total = currentState.totalBytes
                            val speed = currentState.speedBytesPerSecond

                            Column(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.padding(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = String.format("%.1f%%", progress * 100),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${formatBytes(downloaded)} / ${formatBytes(total)}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.padding(8.dp))
                                if (speed > 0) {
                                    Text(
                                        text = "Speed: ${formatBytes(speed)}/s",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is DownloadState.Completed -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = stringResource(id = R.string.model_download_complete),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.padding(8.dp))
                                Text(
                                    text = "Model saved to internal storage",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        is DownloadState.Error -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Download Failed",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.padding(8.dp))
                                Text(
                                    text = currentState.message,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.padding(24.dp))
                                Button(
                                    onClick = { viewModel.retryDownload() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.model_download_retry),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        is DownloadState.Paused -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { currentState.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.padding(16.dp))
                                Text(
                                    text = "Download Paused - ${String.format("%.1f%%", currentState.progress * 100)}",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
