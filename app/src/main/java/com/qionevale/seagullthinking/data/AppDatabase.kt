package com.qionevale.seagullthinking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AdRecord::class, AnalysisItem::class, Quote::class, MusicMode::class, Track::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun adRecordDao(): AdRecordDao
    abstract fun analysisItemDao(): AnalysisItemDao
    abstract fun quoteDao(): QuoteDao
    abstract fun musicModeDao(): MusicModeDao
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seagull.db"
                ).build().also { INSTANCE = it }
            }
    }
}
