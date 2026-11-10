package com.audioscribe.poc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.audioscribe.poc.data.model.TranscriptEntity
import com.audioscribe.poc.data.db.converters.Converters

@Database(
    entities = [TranscriptEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TranscriptDatabase : RoomDatabase() {
    
    abstract fun transcriptDao(): TranscriptDao
    
    companion object {
        @Volatile
        private var INSTANCE: TranscriptDatabase? = null
        
        fun getDatabase(context: Context): TranscriptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TranscriptDatabase::class.java,
                    "transcript_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
