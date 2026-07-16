package com.lab.lab04eq.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lab.lab04eq.data.remote.NetworkConstants
import com.lab.lab04eq.data.remote.RetrofitClient
import com.lab.lab04eq.data.remote.model.GeoEventRequest
import com.lab.lab04eq.data.remote.model.GeoEventResponse
import com.lab.lab04eq.data.remote.model.GpsSyncRequest
import com.lab.lab04eq.data.repository.AudioRepository
import com.lab.lab04eq.data.repository.GpsRepository
import com.lab.lab04eq.data.repository.MediaRepository
import com.lab.lab04eq.data.session.SessionManager
import com.lab.lab04eq.workers.DelayedNotificationWorker
import android.os.Build
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

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
    private val audioRepository: AudioRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    private val _lastWorkId = MutableStateFlow<UUID?>(null)
    val lastWorkId = _lastWorkId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress = _syncProgress.asStateFlow()

    private val _cloudRecords = MutableStateFlow<List<GeoEventResponse>>(emptyList())
    val cloudRecords = _cloudRecords.asStateFlow()

    private val _isLoadingCloud = MutableStateFlow(false)
    val isLoadingCloud = _isLoadingCloud.asStateFlow()

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

    fun sync(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = 0f
            _syncMessage.value = "Iniciando sincronización..."
            try {
                val googlePoints = gpsRepository.googlePoints.first()
                val sensorsPoints = gpsRepository.sensorsPoints.first()

                val deviceId = sessionManager.getDeviceId()
                val userId = sessionManager.userId.first()
                val token = sessionManager.accessToken.first()
                val authHeader = if (token != null) "Bearer $token" else null

                if (userId == null) {
                    _syncMessage.value = "Error: No se encontró el ID de usuario. Por favor, cierra sesión e inicia sesión de nuevo."
                    _isSyncing.value = false
                    onResult(false)
                    return@launch
                }

                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val formatter = DateTimeFormatter.ISO_INSTANT
                var successCount = 0
                val totalToSync = googlePoints.size + sensorsPoints.size

                if (totalToSync == 0) {
                    _syncMessage.value = "No hay datos para sincronizar"
                    _isSyncing.value = false
                    _syncProgress.value = 1f
                    onResult(true)
                    return@launch
                }

                var currentItem = 0
                googlePoints.forEach { point ->
                    if (point.latitud != 0.0) {
                        val request = GeoEventRequest(
                            userId = userId,
                            latitude = point.latitud,
                            longitude = point.longitud,
                            accuracy = point.precision.toDouble(),
                            speed = 0.0, // GpsGoogleEntity doesn't have speed in this project?
                            heading = 0.0, // or bearing
                            eventType = "gps_google",
                            deviceId = deviceId,
                            appVersion = "1.0.0",
                            deviceModel = deviceModel,
                            recordedAt = formatter.format(Instant.ofEpochMilli(point.timestamp))
                        )
                        val response = RetrofitClient.apiService.createGeoEventORM(NetworkConstants.PROJECT_SLUG, authHeader, request)
                        if (response.isSuccessful) successCount++
                    }
                    currentItem++
                    _syncProgress.value = currentItem.toFloat() / totalToSync
                }

                sensorsPoints.forEach { point ->
                    if (point.latitud != 0.0) {
                        val request = GeoEventRequest(
                            userId = userId,
                            latitude = point.latitud,
                            longitude = point.longitud,
                            altitude = point.altitud,
                            eventType = "gps_sensors",
                            deviceId = deviceId,
                            appVersion = "1.0.0",
                            deviceModel = deviceModel,
                            recordedAt = formatter.format(Instant.ofEpochMilli(point.timestamp))
                        )
                        val response = RetrofitClient.apiService.createGeoEventORM(NetworkConstants.PROJECT_SLUG, authHeader, request)
                        if (response.isSuccessful) successCount++
                    }
                    currentItem++
                    _syncProgress.value = currentItem.toFloat() / totalToSync
                }

                if (successCount > 0) {
                    gpsRepository.clearAll()
                    _syncMessage.value = "Sincronizados $successCount registros con éxito"
                    refreshCloudData()
                } else {
                    _syncMessage.value = "Error al sincronizar con el servidor"
                }
                onResult(successCount > 0)
            } catch (e: Exception) {
                _syncMessage.value = "Error: ${e.localizedMessage}"
                onResult(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun refreshCloudData() {
        viewModelScope.launch {
            _isLoadingCloud.value = true
            try {
                // Bug intencional mencionado en la guía (sección 16/20):
                // Se usa currentUsername (email) en lugar de userId (UUID)
                val userIdForFilter = sessionManager.currentUsername.first()
                val token = sessionManager.accessToken.first()
                val authHeader = if (token != null) "Bearer $token" else null

                val response = RetrofitClient.apiService.listGeoEventsORM(
                    NetworkConstants.PROJECT_SLUG,
                    authHeader,
                    userId = userIdForFilter,
                    limit = 10
                )

                if (response.isSuccessful) {
                    _cloudRecords.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Silencioso
            } finally {
                _isLoadingCloud.value = false
            }
        }
    }

    fun forceSync(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // Obtenemos el slug dinámico persistido (Ejercicio 5)
                val slug = sessionManager.projectSlug.firstOrNull() ?: NetworkConstants.PROJECT_SLUG

                // 1. Sincronizar GPS (Google)
                val googlePoints = gpsRepository.googlePoints.firstOrNull() ?: emptyList()
                if (googlePoints.isNotEmpty()) {
                    val gpsRequests = googlePoints.map {
                        GpsSyncRequest(
                            latitud = it.latitud,
                            longitud = it.longitud,
                            altitud = it.altitud,
                            precision = it.precision,
                            provider = "google_flp",
                            timestamp = it.timestamp
                        )
                    }
                    // Ya no pasamos el token manualmente (Ejercicio 2)
                    RetrofitClient.apiService.syncGps(slug, gpsRequests)
                }

                // 2. Sincronizar Multimedia (Fotos y Videos)
                val mediaItems = mediaRepository.allMedia.firstOrNull() ?: emptyList()
                for (item in mediaItems) {
                    val file = File(item.rutaArchivo)
                    if (file.exists()) {
                        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                        val tipo = item.tipo.toRequestBody("text/plain".toMediaTypeOrNull())
                        val ts = item.timestamp.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                        
                        RetrofitClient.apiService.uploadMedia(slug, body, tipo, ts)
                    }
                }

                // 3. Sincronizar Audios
                val audios = audioRepository.allAudios.firstOrNull() ?: emptyList()
                for (audio in audios) {
                    val file = File(audio.rutaArchivo)
                    if (file.exists()) {
                        val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                        val tipo = "AUDIO".toRequestBody("text/plain".toMediaTypeOrNull())
                        val ts = audio.timestamp.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                        RetrofitClient.apiService.uploadMedia(slug, body, tipo, ts)
                    }
                }

                onResult(true, "Sincronización completada con éxito")
            } catch (e: Exception) {
                Log.e("SyncViewModel", "Error en sincronización", e)
                onResult(false, "Error: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun scheduleDelayedNotification(title: String, message: String) {
        val inputData = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .build()

        val notificationRequest = OneTimeWorkRequestBuilder<DelayedNotificationWorker>()
            .setInputData(inputData)
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        _lastWorkId.value = notificationRequest.id
        workManager.enqueue(notificationRequest)
    }

    fun cancelDelayedNotification() {
        _lastWorkId.value?.let { uuid ->
            workManager.cancelWorkById(uuid)
            _lastWorkId.value = null
        }
    }

    class Factory(
        private val context: Context,
        private val gpsRepository: GpsRepository,
        private val mediaRepository: MediaRepository,
        private val audioRepository: AudioRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SyncViewModel::class.java)) {
                return SyncViewModel(context, gpsRepository, mediaRepository, audioRepository, sessionManager) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
        }
    }
}
