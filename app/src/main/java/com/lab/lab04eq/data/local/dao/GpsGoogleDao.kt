package com.lab.lab04eq.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lab.lab04eq.data.local.entity.GpsGoogleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsGoogleDao {
    @Insert
    suspend fun insert(ubicacion: GpsGoogleEntity)

    @Query("SELECT * FROM gps_google_updates ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<GpsGoogleEntity>>

    @Query("SELECT COUNT(*) FROM gps_google_updates")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM gps_google_updates")
    suspend fun deleteAll()
}