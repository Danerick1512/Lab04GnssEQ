package com.lab.lab04eq.data.repository

import com.lab.lab04eq.data.local.FileStorageManager
import com.lab.lab04eq.data.local.dao.MediaDao
import com.lab.lab04eq.data.local.entity.MediaEntity
import com.lab.lab04eq.data.local.entity.MediaType
import kotlinx.coroutines.flow.Flow

class MediaRepository(
    private val mediaDao: MediaDao,
    private val fileStorage: FileStorageManager
) {
    val allMedia: Flow<List<MediaEntity>> = mediaDao.observeAll()
    val photoCount: Flow<Int> = mediaDao.observePhotoCount()
    val videoCount: Flow<Int> = mediaDao.observeVideoCount()

    suspend fun registerPhoto(
        filePath: String,
        widthPx: Int,
        heightPx: Int
    ): Long = mediaDao.insert(
        MediaEntity(
            rutaArchivo = filePath,
            tipo        = MediaType.PHOTO.name,
            tamanoBytes = fileStorage.fileSize(filePath),
            anchoPx     = widthPx,
            altoPx      = heightPx,
            timestamp   = System.currentTimeMillis()
        )
    )

    suspend fun registerVideo(
        filePath: String,
        durationMs: Long
    ): Long = mediaDao.insert(
        MediaEntity(
            rutaArchivo = filePath,
            tipo        = MediaType.VIDEO.name,
            tamanoBytes = fileStorage.fileSize(filePath),
            duracionMs  = durationMs,
            timestamp   = System.currentTimeMillis()
        )
    )

    suspend fun delete(item: MediaEntity) {
        fileStorage.deleteFile(item.rutaArchivo)
        mediaDao.delete(item)
    }
}