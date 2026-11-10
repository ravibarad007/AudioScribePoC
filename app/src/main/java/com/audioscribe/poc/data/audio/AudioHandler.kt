package com.audioscribe.poc.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class AudioHandler(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private val isRecording = AtomicBoolean(false)
    private var currentAudioFile: File? = null
    private var recordingStartTime: Long = 0
    
    private val audioDir: File = File(context.filesDir, "audio").apply {
        if (!exists()) mkdirs()
    }
    
    companion object {
        private const val TAG = "AudioHandler"
        private const val AUDIO_FORMAT = MediaRecorder.OutputFormat.MPEG_4
        private const val AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
        private const val SAMPLE_RATE = 16000 // Whisper prefers 16kHz
        private const val CHANNELS = 1 // Mono
        private const val BIT_RATE = 64000 // 64 kbps
    }
    
    /**
     * Start recording audio to a new file
     * @return Result with file path on success, error message on failure
     */
    fun startRecording(): Result<String> {
        if (isRecording.get()) {
            return Result.failure(IllegalStateException("Already recording"))
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_$timestamp.m4a"
        currentAudioFile = File(audioDir, fileName)
        
        mediaRecorder = MediaRecorder().apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(AUDIO_FORMAT)
                setAudioEncoder(AUDIO_ENCODER)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioChannels(CHANNELS)
                setAudioEncodingBitRate(BIT_RATE)
                setOutputFile(currentAudioFile!!.absolutePath)
                prepare()
                start()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                release()
                return@apply Result.failure(e)
            }
        }
        
        isRecording.set(true)
        recordingStartTime = System.currentTimeMillis()
        
        return Result.success(currentAudioFile!!.absolutePath)
    }
    
    /**
     * Stop recording and return the audio file path
     */
    fun stopRecording(): Result<String> {
        if (!isRecording.getAndSet(false)) {
            return Result.failure(IllegalStateException("Not recording"))
        }
        
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                // Recording may have been stopped already
                Log.w(TAG, "Recorder stop failed", e)
            }
            release()
        }
        mediaRecorder = null
        
        val file = currentAudioFile
        currentAudioFile = null
        
        return if (file != null && file.exists() && file.length() > 0) {
            Result.success(file.absolutePath)
        } else {
            Result.failure(IllegalStateException("Recording file is empty or missing"))
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isCurrentlyRecording(): Boolean = isRecording.get()
    
    /**
     * Get current recording duration in milliseconds
     */
    fun getRecordingDuration(): Long {
        if (!isRecording.get()) return 0
        return System.currentTimeMillis() - recordingStartTime
    }
    
    /**
     * Get current recording file path (if recording)
     */
    fun getCurrentRecordingPath(): String? {
        return currentAudioFile?.absolutePath
    }
    
    /**
     * Import audio file from a content URI (file picker)
     * Copies the file to internal storage for Whisper to access
     */
    suspend fun importAudioFromUri(uri: Uri): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "imported_$timestamp.m4a"
            val destFile = File(audioDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("Could not open input stream"))
            
            if (!destFile.exists() || destFile.length() == 0) {
                destFile.delete()
                return@withContext Result.failure(IllegalStateException("Imported file is empty"))
            }
            
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get list of all recorded/imported audio files
     */
    fun getAudioFiles(): List<File> {
        return audioDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * Delete an audio file
     */
    fun deleteAudioFile(file: File): Boolean {
        return file.delete()
    }
    
    /**
     * Delete audio file by path
     */
    fun deleteAudioFile(path: String): Boolean {
        return File(path).delete()
    }
    
    /**
     * Release resources
     */
    fun release() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                // Ignore
            }
            release()
        }
        mediaRecorder = null
        isRecording.set(false)
        currentAudioFile = null
    }
    
    /**
     * Get audio directory path
     */
    fun getAudioDirectory(): File = audioDir
}
