package com.qionevale.seagullthinking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AdRecord::class,
        AnalysisItem::class,
        Quote::class,
        MusicMode::class,
        Track::class,
        AdAsset::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun adRecordDao(): AdRecordDao
    abstract fun analysisItemDao(): AnalysisItemDao
    abstract fun quoteDao(): QuoteDao
    abstract fun musicModeDao(): MusicModeDao
    abstract fun trackDao(): TrackDao
    abstract fun adAssetDao(): AdAssetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 1 → 2：广告改成"素材库"直连，新增 ad_assets；
         * 语录支持字体/加粗/斜体，三列可空，旧数据不受影响。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ad_assets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`ref` TEXT NOT NULL, " +
                        "`addedAt` INTEGER NOT NULL, " +
                        "`lastPlayedAt` INTEGER NOT NULL, " +
                        "`playCount` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE `quotes` ADD COLUMN `fontKey` TEXT")
                db.execSQL("ALTER TABLE `quotes` ADD COLUMN `bold` INTEGER")
                db.execSQL("ALTER TABLE `quotes` ADD COLUMN `italic` INTEGER")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seagull.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
