package com.lab.lab04eq.data.repository

import com.lab.lab04eq.data.local.FileStorageManager
import com.lab.lab04eq.data.local.dao.AudioDao
import com.lab.lab04eq.data.local.entity.AudioEntity
import kotlinx.coroutines.flow.Flow

class AudioRepository(
    private val audioDao: AudioDao,
    private val fileStorage: FileStorageManager
) {
    val allAudios: Flow<List<AudioEntity>> = audioDao.observeAll()
    val count: Flow<Int> = audioDao.observeCount()

    suspend fun registerAudio(
        filePath: String,
        durationMs: Long,
        format: String = "AAC"
    ): Long = audioDao.insert(
        AudioEntity(
            rutaArchivo = filePath,
            duracionMs  = durationMs,
            tamanoBytes = fileStorage.fileSize(filePath),
            formato     = format,
            timestamp   = System.currentTimeMillis()
        )
    )

    suspend fun delete(item: AudioEntity) {
        fileStorage.deleteFile(item.rutaArchivo)
        audioDao.delete(item)
    }
}