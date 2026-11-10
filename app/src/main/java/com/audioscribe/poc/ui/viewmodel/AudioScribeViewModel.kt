package com.audioscribe.poc.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.audioscribe.poc.data.audio.AudioHandler
import com.audioscribe.poc.data.db.TranscriptDatabase
import com.audioscribe.poc.data.db.TranscriptDao
import com.audioscribe.poc.data.download.ModelDownloadManager
import com.audioscribe.poc.data.download.DownloadState
import com.audioscribe.poc.data.model.TranscriptEntity
import com.audioscribe.poc.data.transcription.WhisperTranscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asLiveData
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioScribeViewModel(application: Application) : AndroidViewModel(application) {
    
    // Data layer components
    private val transcriptDao: TranscriptDao = TranscriptDatabase.getDatabase(application).transcriptDao()
    private val modelDownloadManager: ModelDownloadManager = ModelDownloadManager.getInstance(application)
    private val audioHandler: AudioHandler = AudioHandler(application)
    private val whisperTranscriber: WhisperTranscriber = WhisperTranscriber.getInstance(application)
    
    // UI State
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle(modelDownloadManager.isModelDownloaded()))
    val downloadState: LiveData<DownloadState> = _downloadState.asLiveData()
    
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: LiveData<Boolean> = _isModelReady.asLiveData()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: LiveData<Boolean> = _isRecording.asLiveData()
    
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: LiveData<Long> = _recordingDuration.asLiveData()
    
    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: LiveData<TranscriptionState> = _transcriptionState.asLiveData()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: LiveData<String> = _searchQuery.asLiveData()
    
    // Transcripts with search filtering
    private val allTranscripts = transcriptDao.getAllTranscripts()
    private val searchTranscripts = transcriptDao.searchTranscripts("")
    
    val transcripts: LiveData<List<TranscriptEntity>> = combine(
        allTranscripts,
        searchQuery.distinctUntilChanged()
    ) { transcripts, query ->
        if (query.isBlank()) {
            transcripts
        } else {
            transcripts.filter { transcript ->
                transcript.transcriptText.contains(query, ignoreCase = true) ||
                transcript.title.contains(query, ignoreCase = true)
            }
        }
    }.asLiveData()
    
    init {
        // Initialize Whisper if model exists
        if (modelDownloadManager.isModelDownloaded()) {
            val modelPath = modelDownloadManager.getModelPath()!!
            _isModelReady.value = whisperTranscriber.initialize(modelPath)
        }
        
        // Start recording duration timer
        startRecordingTimer()
    }
    
    // Transcription states
    sealed interface TranscriptionState {
        data class Idle : TranscriptionState
        data class Processing(val progress: String) : TranscriptionState
        data class Completed(val transcript: TranscriptEntity) : TranscriptionState
        data class Error(val message: String) : TranscriptionState
    }
    
    /**
     * Download the Whisper model on first launch
     */
    fun downloadModel() {
        viewModelScope.launch {
            val progressChannel = Channel<DownloadState>(Channel.UNLIMITED)
            
            // Collect progress updates
            val progressJob = viewModelScope.launch {
                for (state in progressChannel) {
                    _downloadState.value = state
                }
            }
            
            // Download model
            val result = modelDownloadManager.downloadModel(progressChannel)
            
            progressChannel.close()
            progressJob.join()
            
            // Handle result
            when (result) {
                is DownloadState.Completed -> {
                    _downloadState.value = result
                    _isModelReady.value = whisperTranscriber.initialize(result.modelPath)
                }
                is DownloadState.Error -> {
                    _downloadState.value = result
                    _isModelReady.value = false
                }
                else -> {}
            }
        }
    }
    
    /**
     * Retry model download
     */
    fun retryDownload() {
        downloadModel()
    }
    
    /**
     * Start audio recording
     */
    fun startRecording(): Result<String> {
        val result = audioHandler.startRecording()
        if (result.isSuccess) {
            _isRecording.value = true
            _recordingDuration.value = 0
        }
        return result
    }
    
    /**
     * Stop audio recording
     */
    fun stopRecording(): Result<String> {
        val result = audioHandler.stopRecording()
        _isRecording.value = false
        _recordingDuration.value = 0
        return result
    }
    
    /**
     * Update recording duration (called from timer)
     */
    private fun updateRecordingDuration() {
        if (_isRecording.value) {
            _recordingDuration.value = audioHandler.getRecordingDuration()
        }
    }
    
    /**
     * Start recording duration timer
     */
    private fun startRecordingTimer() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(100)
                updateRecordingDuration()
            }
        }
    }
    
    /**
     * Import audio file from URI
     */
    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionState.Processing("Importing audio file...")
            
            val result = audioHandler.importAudioFromUri(uri)
            
            if (result.isSuccess) {
                transcribeAudio(result.getOrThrow())
            } else {
                _transcriptionState.value = TranscriptionState.Error(
                    "Failed to import audio: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    /**
     * Transcribe audio file and save to database
     */
    private fun transcribeAudio(audioPath: String) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionState.Processing("Transcribing audio...")
            
            val result = whisperTranscriber.transcribe(audioPath)
            
            when (result) {
                is Result.Success -> {
                    val transcriptText = result.getOrThrow()
                    val title = generateTitleFromText(transcriptText)
                    val duration = estimateDuration(audioPath)
                    
                    val entity = TranscriptEntity(
                        title = title,
                        transcriptText = transcriptText,
                        audioFilePath = audioPath,
                        timestampMillis = System.currentTimeMillis(),
                        durationSeconds = duration
                    )
                    
                    val id = transcriptDao.insert(entity)
                    val savedEntity = entity.copy(id = id)
                    
                    _transcriptionState.value = TranscriptionState.Completed(savedEntity)
                }
                is Result.Failure -> {
                    _transcriptionState.value = TranscriptionState.Error(
                        "Transcription failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Transcribe a recorded audio file
     */
    fun transcribeRecording(audioPath: String) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionState.Processing("Transcribing recording...")
            val result = whisperTranscriber.transcribe(audioPath)
            
            when (result) {
                is Result.Success -> {
                    val transcriptText = result.getOrThrow()
                    val title = generateTitleFromText(transcriptText)
                    val duration = estimateDuration(audioPath)
                    
                    val entity = TranscriptEntity(
                        title = title,
                        transcriptText = transcriptText,
                        audioFilePath = audioPath,
                        timestampMillis = System.currentTimeMillis(),
                        durationSeconds = duration
                    )
                    
                    val id = transcriptDao.insert(entity)
                    val savedEntity = entity.copy(id = id)
                    
                    _transcriptionState.value = TranscriptionState.Completed(savedEntity)
                }
                is Result.Failure -> {
                    _transcriptionState.value = TranscriptionState.Error(
                        "Transcription failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Search transcripts
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Delete a transcript
     */
    fun deleteTranscript(transcript: TranscriptEntity) {
        viewModelScope.launch {
            transcriptDao.delete(transcript)
            // Also delete associated audio file
            audioHandler.deleteAudioFile(transcript.audioFilePath)
        }
    }
    
    /**
     * Get transcript by ID
     */
    suspend fun getTranscriptById(id: Long): TranscriptEntity? {
        return transcriptDao.getTranscriptById(id)
    }
    
    /**
     * Generate a title from transcript text
     */
    private fun generateTitleFromText(text: String): String {
        val words = text.trim().split("\\s+".toRegex())
        val titleWords = words.take(8).joinToString(" ")
        return if (titleWords.length > 50) {
            titleWords.substring(0, 47) + "..."
        } else {
            titleWords
        }.ifEmpty { "Transcript ${java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" }
    }
    
    /**
     * Estimate audio duration (mock implementation)
     */
    private fun estimateDuration(audioPath: String): Int {
        val file = java.io.File(audioPath)
        if (!file.exists()) return 0
        
        // Rough estimate: 64kbps AAC, 16kHz mono
        // 64000 bits/sec = 8000 bytes/sec
        val estimatedSeconds = (file.length() / 8000).toInt()
        return estimatedSeconds.coerceAtLeast(1)
    }
    
    /**
     * Check if currently recording
     */
    fun getIsRecording(): Boolean = _isRecording.value
    
    /**
     * Get current recording duration in milliseconds
     */
    fun getRecordingDurationMs(): Long = _recordingDuration.value
    
    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        audioHandler.release()
        whisperTranscriber.release()
    }
}
