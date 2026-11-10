package com.audioscribe.poc.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    
    val transcriptText: String,
    
    val audioFilePath: String,
    
    val timestampMillis: Long,
    
    val durationSeconds: Int = 0
) {
    // Helper property for display
    val formattedDate: String
        get() = java.text.SimpleDateFormat("MMM d, yyyy • HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestampMillis))
    
    val formattedDuration: String
        get() = String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60)
}
