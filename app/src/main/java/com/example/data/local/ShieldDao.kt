package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShieldDao {
    @Query("SELECT * FROM shield_stats WHERE id = 1 LIMIT 1")
    fun getShieldStats(): Flow<ShieldStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveShieldStats(stats: ShieldStatsEntity)

    @Query("""
        UPDATE shield_stats 
        SET trackersBlocked = trackersBlocked + :trackers,
            adsBlocked = adsBlocked + :ads,
            dataSavedKb = dataSavedKb + :dataKb,
            timeSavedMs = timeSavedMs + :timeMs
        WHERE id = 1
    """)
    suspend fun addBlockedStats(trackers: Long, ads: Long, dataKb: Long, timeMs: Long)

    @Query("INSERT OR IGNORE INTO shield_stats (id, trackersBlocked, adsBlocked, dataSavedKb, timeSavedMs) VALUES (1, 0, 0, 0, 0)")
    suspend fun ensureInitialized()
}
