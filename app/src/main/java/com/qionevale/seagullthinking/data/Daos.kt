package com.qionevale.seagullthinking.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AdRecordDao {
    @Query("SELECT * FROM ad_records ORDER BY watchedAt DESC")
    fun observeAll(): Flow<List<AdRecord>>

    @Query("SELECT * FROM ad_records WHERE watchedAt >= :from AND watchedAt < :to ORDER BY watchedAt ASC")
    fun observeRange(from: Long, to: Long): Flow<List<AdRecord>>

    @Query("SELECT * FROM ad_records ORDER BY watchedAt ASC")
    suspend fun allOnce(): List<AdRecord>

    @Query("SELECT * FROM ad_records WHERE id = :id")
    suspend fun byId(id: Long): AdRecord?

    @Insert
    suspend fun insert(record: AdRecord): Long

    @Update
    suspend fun update(record: AdRecord)

    @Delete
    suspend fun delete(record: AdRecord)

    @Query("DELETE FROM ad_records")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM ad_records")
    fun observeCount(): Flow<Int>
}

@Dao
interface AnalysisItemDao {
    @Query("SELECT * FROM analysis_items WHERE recordId = :recordId ORDER BY orderIndex ASC")
    fun observeFor(recordId: Long): Flow<List<AnalysisItem>>

    @Query("SELECT * FROM analysis_items WHERE recordId = :recordId ORDER BY orderIndex ASC")
    suspend fun forRecord(recordId: Long): List<AnalysisItem>

    @Query("SELECT * FROM analysis_items ORDER BY recordId ASC, orderIndex ASC")
    suspend fun allOnce(): List<AnalysisItem>

    @Query("SELECT * FROM analysis_items ORDER BY recordId ASC, orderIndex ASC")
    fun observeAll(): Flow<List<AnalysisItem>>

    @Insert
    suspend fun insertAll(items: List<AnalysisItem>)

    @Update
    suspend fun update(item: AnalysisItem)

    @Query("DELETE FROM analysis_items WHERE recordId = :recordId")
    suspend fun deleteFor(recordId: Long)

    @Query("DELETE FROM analysis_items")
    suspend fun clear()
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes ORDER BY createdAt ASC")
    suspend fun allOnce(): List<Quote>

    @Insert
    suspend fun insert(quote: Quote): Long

    @Update
    suspend fun update(quote: Quote)

    @Delete
    suspend fun delete(quote: Quote)

    @Query("DELETE FROM quotes")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun count(): Int
}

@Dao
interface AdAssetDao {
    @Query("SELECT * FROM ad_assets ORDER BY lastPlayedAt ASC, addedAt ASC")
    fun observeAll(): Flow<List<AdAsset>>

    @Query("SELECT * FROM ad_assets ORDER BY lastPlayedAt ASC, addedAt ASC")
    suspend fun allOnce(): List<AdAsset>

    @Insert
    suspend fun insert(asset: AdAsset): Long

    @Update
    suspend fun update(asset: AdAsset)

    @Delete
    suspend fun delete(asset: AdAsset)

    @Query("DELETE FROM ad_assets")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM ad_assets")
    suspend fun count(): Int
}

@Dao
interface MusicModeDao {
    @Query("SELECT * FROM music_modes ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<MusicMode>>

    @Insert
    suspend fun insert(mode: MusicMode): Long

    @Update
    suspend fun update(mode: MusicMode)

    @Delete
    suspend fun delete(mode: MusicMode)

    @Query("DELETE FROM music_modes")
    suspend fun clear()
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE modeId = :modeId ORDER BY addedAt ASC")
    fun observeForMode(modeId: Long): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE modeId = :modeId ORDER BY addedAt ASC")
    suspend fun forMode(modeId: Long): List<Track>

    @Query("SELECT * FROM tracks ORDER BY addedAt ASC")
    suspend fun allOnce(): List<Track>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun byId(id: Long): Track?

    @Insert
    suspend fun insert(track: Track): Long

    @Update
    suspend fun update(track: Track)

    @Delete
    suspend fun delete(track: Track)

    @Query("DELETE FROM tracks WHERE modeId = :modeId")
    suspend fun deleteForMode(modeId: Long)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :id")
    suspend fun bumpPlayCount(id: Long)

    @Query("UPDATE tracks SET listenedMs = listenedMs + :delta WHERE id = :id")
    suspend fun addListened(id: Long, delta: Long)

    @Query("DELETE FROM tracks")
    suspend fun clear()
}
