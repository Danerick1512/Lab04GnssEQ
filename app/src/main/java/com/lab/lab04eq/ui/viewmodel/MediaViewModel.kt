package com.lab.lab04eq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lab.lab04eq.data.local.FileStorageManager
import com.lab.lab04eq.data.local.entity.MediaEntity
import com.lab.lab04eq.data.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MediaViewModel(
    private val mediaRepository: MediaRepository,
    private val fileStorage: FileStorageManager
) : ViewModel() {

    // Lista observable en tiempo real de todos los elementos multimedia (fotos y videos) en Room
    val mediaItems = mediaRepository.allMedia.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    // Conteos observables para mostrar estadísticas en la UI
    val photoCount = mediaRepository.photoCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0
    )

    val videoCount = mediaRepository.videoCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0
    )

    /**
     * Prepara y retorna un archivo vacío en el almacenamiento privado listo para que la cámara guarde una foto.
     */
    fun preparePhotoFile(): File {
        return fileStorage.newPhotoFile()
    }

    /**
     * Prepara y retorna un archivo vacío en el almacenamiento privado listo para que la cámara grabe un video.
     */
    fun prepareVideoFile(): File {
        return fileStorage.newVideoFile()
    }

    /**
     * Registra de forma oficial una foto tomada con éxito en la base de datos de Room.
     * Nota: Como las cámaras por Intent nativo no retornan resolución exacta directamente,
     * se envían dimensiones base en 0 o por defecto (pueden ser leídas después si se requiere).
     */
    fun registerPhoto(file: File) {
        viewModelScope.launch {
            if (file.exists() && file.length() > 0) {
                mediaRepository.registerPhoto(
                    filePath = file.absolutePath,
                    widthPx = 0,
                    heightPx = 0
                )
            }
        }
    }

    /**
     * Registra de forma oficial un video grabado con éxito en la base de datos de Room.
     */
    fun registerVideo(file: File) {
        viewModelScope.launch {
            if (file.exists() && file.length() > 0) {
                // Registra el video calculando su peso real.
                // El laboratorio asume una duración aproximada o resuelta en reproducción.
                mediaRepository.registerVideo(
                    filePath = file.absolutePath,
                    durationMs = 0L
                )
            }
        }
    }

    /**
     * Borra el registro de Room y elimina físicamente el archivo del almacenamiento del celular.
     */
    fun deleteMedia(item: MediaEntity) {
        viewModelScope.launch {
            mediaRepository.delete(item)
        }
    }
}

/**
 * Factory obligatorio para la correcta inyección manual de dependencias desde tu Lab04EqApp.
 */
class MediaViewModelFactory(
    private val mediaRepository: MediaRepository,
    private val fileStorage: FileStorageManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            return MediaViewModel(mediaRepository, fileStorage) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}