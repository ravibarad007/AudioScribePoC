package com.audioscribe.poc.data.download

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

// State for model download
sealed interface DownloadState {
    data class Idle(val modelExists: Boolean) : DownloadState
    data class Downloading(
        val progress: Float, // 0.0 to 1.0
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long
    ) : DownloadState
    data class Completed(val modelPath: String) : DownloadState
    data class Error(val message: String) : DownloadState
    data class Paused(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState
}

class ModelDownloadManager private constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    
    companion object {
        // Model file name
        private const val MODEL_FILE_NAME = "ggml-base.bin"
        
        // Download URL for Whisper base model (from Hugging Face)
        private const val MODEL_DOWNLOAD_URL = 
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
        
        // Expected file size (~74MB) for validation
        private const val EXPECTED_MODEL_SIZE = 74 * 1024 * 1024 // ~74MB
        
        @Volatile
        private var INSTANCE: ModelDownloadManager? = null
        
        fun getInstance(context: Context): ModelDownloadManager {
            return INSTANCE ?: synchronized(this) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS) // 5 minutes for large file
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val instance = ModelDownloadManager(context.applicationContext, client)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val modelDir: File = File(context.filesDir, "models")
    private val modelFile: File = File(modelDir, MODEL_FILE_NAME)
    private val tempModelFile: File = File(modelDir, "${MODEL_FILE_NAME}.tmp")
    
    init {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
    }
    
    /**
     * Check if model already exists and is valid
     */
    fun isModelDownloaded(): Boolean {
        return modelFile.exists() && modelFile.length() > EXPECTED_MODEL_SIZE / 2 // At least half expected size
    }
    
    /**
     * Get the model file path if it exists
     */
    fun getModelPath(): String? {
        return if (isModelDownloaded()) modelFile.absolutePath else null
    }
    
    /**
     * Download the model with progress updates
     * @param progressChannel Channel to receive download progress updates
     */
    suspend fun downloadModel(
        progressChannel: Channel<DownloadState>
    ): DownloadState = withContext(Dispatchers.IO) {
        
        // If already downloaded, return completed state
        if (isModelDownloaded()) {
            return@withContext DownloadState.Completed(modelFile.absolutePath)
        }
        
        // Clean up any previous temp file
        if (tempModelFile.exists()) {
            tempModelFile.delete()
        }
        
        val request = Request.Builder()
            .url(MODEL_DOWNLOAD_URL)
            .build()
        
        var response: Response? = null
        var fileOutputStream: FileOutputStream? = null
        
        try {
            response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext DownloadState.Error(
                    "Server error: ${response.code} ${response.message}"
                )
            }
            
            val responseBody = response.body
                ?: return@withContext DownloadState.Error("Empty response body")
            
            val totalBytes = responseBody.contentLength()
            if (totalBytes <= 0) {
                return@withContext DownloadState.Error("Invalid content length")
            }
            
            fileOutputStream = FileOutputStream(tempModelFile)
            
            val source = responseBody.source()
            var bytesDownloaded = 0L
            val buffer = okio.Buffer()
            var lastUpdateTime = System.currentTimeMillis()
            var lastBytesDownloaded = 0L
            
            // Send initial progress
            progressChannel.send(DownloadState.Downloading(
                progress = 0f,
                bytesDownloaded = 0,
                totalBytes = totalBytes,
                speedBytesPerSecond = 0
            ))
            
            while (true) {
                val read = source.read(buffer, 8192)
                if (read == -1L) break // EOF
                
                val bytes = buffer.readByteArray(read)
                fileOutputStream.write(bytes)
                bytesDownloaded += read
                
                // Calculate speed and send progress updates (throttled to ~4 FPS)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= 250) {
                    val timeDelta = (currentTime - lastUpdateTime) / 1000.0
                    val bytesDelta = bytesDownloaded - lastBytesDownloaded
                    val speed = if (timeDelta > 0) (bytesDelta / timeDelta).toLong() else 0L
                    
                    progressChannel.send(DownloadState.Downloading(
                        progress = bytesDownloaded.toFloat() / totalBytes,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        speedBytesPerSecond = speed
                    ))
                    
                    lastUpdateTime = currentTime
                    lastBytesDownloaded = bytesDownloaded
                }
            }
            
            // Final progress update
            progressChannel.send(DownloadState.Downloading(
                progress = 1f,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                speedBytesPerSecond = 0
            ))
            
            fileOutputStream.flush()
            fileOutputStream.close()
            fileOutputStream = null
            responseBody.close()
            
            // Verify download
            if (tempModelFile.length() < EXPECTED_MODEL_SIZE / 2) {
                tempModelFile.delete()
                return@withContext DownloadState.Error("Downloaded file is too small, may be corrupted")
            }
            
            // Move temp file to final location
            if (modelFile.exists()) {
                modelFile.delete()
            }
            val success = tempModelFile.renameTo(modelFile)
            if (!success) {
                return@withContext DownloadState.Error("Failed to save model file")
            }
            
            DownloadState.Completed(modelFile.absolutePath)
            
        } catch (e: IOException) {
            DownloadState.Error("Download failed: ${e.message}")
        } catch (e: Exception) {
            DownloadState.Error("Unexpected error: ${e.message}")
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                // Ignore
            }
            try {
                response?.body?.close()
            } catch (e: IOException) {
                // Ignore
            }
            // Clean up temp file on error
            if (tempModelFile.exists()) {
                tempModelFile.delete()
            }
        }
    }
    
    /**
     * Delete the downloaded model (for testing or reset)
     */
    fun deleteModel(): Boolean {
        return modelFile.delete()
    }
    
    /**
     * Get model file size in human readable format
     */
    fun getModelSizeString(): String {
        val bytes = modelFile.length()
        return Formatter.formatBytes(bytes)
    }
    
    object Formatter {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
                bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
}
