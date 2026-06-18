package com.lab.lab04eq.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lab.lab04eq.data.repository.AudioRepository
import com.lab.lab04eq.data.repository.GpsRepository
import com.lab.lab04eq.data.repository.MediaRepository
import com.lab.lab04eq.workers.DelayedNotificationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import java.util.concurrent.TimeUnit

// Clase de datos limpia para representar el estado consolidado de registros en la app
data class SyncCounts(
    val gpsGoogle: Int = 0,
    val gpsSensors: Int = 0,
    val photos: Int = 0,
    val videos: Int = 0,
    val audios: Int = 0,
    val total: Int = 0
)

class SyncViewModel(
    context: Context,
    private val gpsRepository: GpsRepository,
    private val mediaRepository: MediaRepository,
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    // Estado para rastrear el último WorkRequest programado
    private val _lastWorkId = MutableStateFlow<UUID?>(null)
    val lastWorkId = _lastWorkId.asStateFlow()

    // Fusión reactiva de 5 flujos diferentes para producir las estadísticas unificadas del Laboratorio
    val syncCounts: StateFlow<SyncCounts> = combine(
        gpsRepository.googleCount,
        gpsRepository.sensorsCount,
        mediaRepository.photoCount,
        mediaRepository.videoCount,
        audioRepository.count
    ) { google, sensors, photos, videos, audios ->
        SyncCounts(
            gpsGoogle = google,
            gpsSensors = sensors,
            photos = photos,
            videos = videos,
            audios = audios,
            total = google + sensors + photos + videos + audios
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncCounts()
    )

    /**
     * Agenda una notificación en segundo plano para ejecutarse exactamente 10 segundos después
     */
    fun scheduleDelayedNotification(title: String, message: String) {
        val inputData = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .build()

        // Restricción de ejecución diferida de un solo tiro (10 segundos de delay)
        val notificationRequest = OneTimeWorkRequestBuilder<DelayedNotificationWorker>()
            .setInputData(inputData)
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        _lastWorkId.value = notificationRequest.id
        workManager.enqueue(notificationRequest)
    }

    /**
     * Cancela la última notificación programada si aún no se ha ejecutado
     */
    fun cancelDelayedNotification() {
        _lastWorkId.value?.let { uuid ->
            workManager.cancelWorkById(uuid)
            _lastWorkId.value = null
        }
    }

    /**
     * Factory obligatorio para inyectar los repositorios globales desde tu Lab04EqApp
     */
    class Factory(
        private val context: Context,
        private val gpsRepository: GpsRepository,
        private val mediaRepository: MediaRepository,
        private val audioRepository: AudioRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SyncViewModel::class.java)) {
                return SyncViewModel(context, gpsRepository, mediaRepository, audioRepository) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
        }
    }
}
