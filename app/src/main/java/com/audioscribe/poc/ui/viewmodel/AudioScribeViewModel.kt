package com.audioscribe.poc.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.audioscribe.poc.data.audio.AudioHandler
import com.audioscribe.poc.data.db.TranscriptDatabase
import com.audioscribe.poc.data.db.TranscriptDao
import com.audioscribe.poc.data.download.ModelDownloadManager
import com.audioscribe.poc.data.download.DownloadState
import com.audioscribe.poc.data.model.TranscriptEntity
import com.audioscribe.poc.data.transcription.WhisperTranscriber
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AudioScribeViewModel(application: Application) : AndroidViewModel(application) {

    private val transcriptDao: TranscriptDao = TranscriptDatabase.getDatabase(application).transcriptDao()
    private val modelDownloadManager: ModelDownloadManager = ModelDownloadManager.getInstance(application)
    private val audioHandler: AudioHandler = AudioHandler(application)
    private val whisperTranscriber: WhisperTranscriber = WhisperTranscriber.getInstance(application)

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle())
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

    val transcripts: LiveData<List<TranscriptEntity>> = combine(
        transcriptDao.getAllTranscripts(),
        _searchQuery
    ) { transcriptsList, query ->
        if (query.isBlank()) {
            transcriptsList
        } else {
            transcriptsList.filter { transcript ->
                transcript.transcriptText.contains(query, ignoreCase = true) ||
                transcript.title.contains(query, ignoreCase = true)
            }
        }
    }.asLiveData()

    init {
        if (modelDownloadManager.isModelDownloaded()) {
            val modelPath = modelDownloadManager.getModelPath()
            if (modelPath != null) {
                _isModelReady.value = whisperTranscriber.initialize(modelPath)
            }
        }
        startRecordingTimer()
    }

    sealed interface TranscriptionState {
        object Idle : TranscriptionState
        data class Processing(val progress: String) : TranscriptionState
        data class Completed(val transcript: TranscriptEntity) : TranscriptionState
        data class Error(val message: String) : TranscriptionState
    }

    fun downloadModel() {
        viewModelScope.launch {
            val progressChannel = Channel<DownloadState>(Channel.UNLIMITED)
            val progressJob = viewModelScope.launch {
                for (state in progressChannel) {
                    _downloadState.value = state
                }
            }
            val result = modelDownloadManager.downloadModel(progressChannel)
            progressChannel.close()
            progressJob.join()

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

    fun retryDownload() = downloadModel()

    fun startRecording(): Result<String> {
        val result = audioHandler.startRecording()
        if (result.isSuccess) {
            _isRecording.value = true
            _recordingDuration.value = 0
        }
        return result
    }

    fun stopRecording(): Result<String> {
        val result = audioHandler.stopRecording()
        _isRecording.value = false
        _recordingDuration.value = 0
        return result
    }

    private fun updateRecordingDuration() {
        if (_isRecording.value) {
            _recordingDuration.value = audioHandler.getRecordingDuration()
        }
    }

    private fun startRecordingTimer() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(100)
                updateRecordingDuration()
            }
        }
    }

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

    private fun transcribeAudio(audioPath: String) {
        viewModelScope.launch {
            _transcriptionState.value = TranscriptionState.Processing("Transcribing audio...")
            val result = whisperTranscriber.transcribe(audioPath)
            if (result.isSuccess) {
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
            } else {
                _transcriptionState.value = TranscriptionState.Error(
                    "Transcription failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun transcribeRecording(audioPath: String) {
        transcribeAudio(audioPath)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteTranscript(transcript: TranscriptEntity) {
        viewModelScope.launch {
            transcriptDao.delete(transcript)
            audioHandler.deleteAudioFile(transcript.audioFilePath)
        }
    }

    private fun generateTitleFromText(text: String): String {
        val words = text.trim().split("\\s+".toRegex())
        val titleWords = words.take(8).joinToString(" ")
        return if (titleWords.length > 50) {
            titleWords.substring(0, 47) + "..."
        } else {
            titleWords
        }.ifEmpty { "Transcript ${java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" }
    }

    private fun estimateDuration(audioPath: String): Int {
        val file = java.io.File(audioPath)
        if (!file.exists()) return 0
        val estimatedSeconds = (file.length() / 8000).toInt()
        return estimatedSeconds.coerceAtLeast(1)
    }

    fun getIsRecording(): Boolean = _isRecording.value
    fun getRecordingDurationMs(): Long = _recordingDuration.value

    override fun onCleared() {
        super.onCleared()
        audioHandler.release()
        whisperTranscriber.release()
    }
}
