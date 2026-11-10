package com.audioscribe.poc.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.audioscribe.poc.data.model.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcript: TranscriptEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transcripts: List<TranscriptEntity>): List<Long>
    
    @Update
    suspend fun update(transcript: TranscriptEntity): Int
    
    @Delete
    suspend fun delete(transcript: TranscriptEntity): Int
    
    @Query("DELETE FROM transcripts WHERE id = :id")
    suspend fun deleteById(id: Long): Int
    
    @Query("SELECT * FROM transcripts ORDER BY timestampMillis DESC")
    fun getAllTranscripts(): Flow<List<TranscriptEntity>>
    
    @Query("SELECT * FROM transcripts ORDER BY timestampMillis DESC")
    suspend fun getAllTranscriptsOnce(): List<TranscriptEntity>
    
    @Query("SELECT * FROM transcripts WHERE transcriptText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestampMillis DESC")
    fun searchTranscripts(query: String): Flow<List<TranscriptEntity>>
    
    @Query("SELECT * FROM transcripts WHERE transcriptText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestampMillis DESC")
    suspend fun searchTranscriptsOnce(query: String): List<TranscriptEntity>
    
    @Query("SELECT * FROM transcripts WHERE id = :id")
    suspend fun getTranscriptById(id: Long): TranscriptEntity?
    
    @Query("SELECT COUNT(*) FROM transcripts")
    suspend fun getTotalCount(): Int
}
