package com.lab.lab04eq.data.repository

import com.lab.lab04eq.data.local.dao.GpsGoogleDao
import com.lab.lab04eq.data.local.dao.GpsSensorsDao
import com.lab.lab04eq.data.local.entity.GpsGoogleEntity
import com.lab.lab04eq.data.local.entity.GpsSensorsEntity
import kotlinx.coroutines.flow.Flow

class GpsRepository(
    private val googleDao: GpsGoogleDao,
    private val sensorsDao: GpsSensorsDao
) {
    val googlePoints: Flow<List<GpsGoogleEntity>> = googleDao.observeAll()
    val sensorsPoints: Flow<List<GpsSensorsEntity>> = sensorsDao.observeAll()

    val googleCount: Flow<Int> = googleDao.observeCount()
    val sensorsCount: Flow<Int> = sensorsDao.observeCount()

    suspend fun saveGooglePoint(point: GpsGoogleEntity) = googleDao.insert(point)
    suspend fun saveSensorsPoint(point: GpsSensorsEntity) = sensorsDao.insert(point)

    suspend fun clearAll() {
        googleDao.deleteAll()
        sensorsDao.deleteAll()
    }
}