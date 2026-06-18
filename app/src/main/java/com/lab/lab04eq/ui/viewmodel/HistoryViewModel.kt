package com.lab.lab04eq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lab.lab04eq.data.local.FileStorageManager
import com.lab.lab04eq.data.repository.AudioRepository
import com.lab.lab04eq.data.repository.GpsRepository
import com.lab.lab04eq.data.repository.MediaRepository
import com.lab.lab04eq.model.ActivityItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HistoryViewModel(
    private val gpsRepository: GpsRepository,
    private val mediaRepository: MediaRepository,
    private val audioRepository: AudioRepository,
    private val fileStorage: FileStorageManager
) : ViewModel() {

    // Mezcla reactiva y polimórfica de todas las fuentes de datos ordenadas cronológicamente
    val historyItems: StateFlow<List<ActivityItem>> = combine(
        gpsRepository.googlePoints,   // Transforma las coordenadas de Google Play Services
        gpsRepository.sensorsPoints,  // Transforma las coordenadas de los Sensores Internos
        mediaRepository.allMedia,     // Transforma las Fotos y Videos de la Cámara
        audioRepository.allAudios     // Transforma las notas de Voz del Micrófono
    ) { googleList, sensorsList, mediaList, audioList ->

        val unifiedList = mutableListOf<ActivityItem>()

        // 1. Mapeo de ubicaciones Google
        googleList.forEach { entity ->
            unifiedList.add(
                ActivityItem.GpsGoogle(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    latitud = entity.latitud,
                    longitud = entity.longitud
                )
            )
        }

        // 2. Mapeo de ubicaciones Sensores
        sensorsList.forEach { entity ->
            unifiedList.add(
                ActivityItem.GpsSensors(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    latitud = entity.latitud,
                    longitud = entity.longitud
                )
            )
        }

        // 3. Mapeo discriminado de Multimedia (Fotos o Videos)
        mediaList.forEach { entity ->
            if (entity.tipo == "PHOTO") {
                unifiedList.add(
                    ActivityItem.Photo(
                        id = entity.id,
                        timestamp = entity.timestamp,
                        rutaArchivo = entity.rutaArchivo
                    )
                )
            } else {
                unifiedList.add(
                    ActivityItem.Video(
                        id = entity.id,
                        timestamp = entity.timestamp,
                        rutaArchivo = entity.rutaArchivo,
                        duracionMs = entity.duracionMs ?: 0L
                    )
                )
            }
        }

        // 4. Mapeo de notas de Audio
        audioList.forEach { entity ->
            unifiedList.add(
                ActivityItem.Audio(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    rutaArchivo = entity.rutaArchivo,
                    duracionMs = entity.duracionMs,
                    formato = entity.formato
                )
            )
        }

        // Ordenación cronológica descendente (el evento más reciente primero)
        unifiedList.sortByDescending { it.timestamp }
        unifiedList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /**
     * Exporta el historial actual a un archivo CSV.
     */
    fun exportToCsv(onResult: (File) -> Unit) {
        viewModelScope.launch {
            val items = historyItems.value
            val csvBuilder = StringBuilder("Timestamp,Tipo,Ruta/Coordenadas\n")
            
            items.forEach { item ->
                val type = item.javaClass.simpleName
                val detail = when (item) {
                    is ActivityItem.GpsGoogle -> "${item.latitud};${item.longitud}"
                    is ActivityItem.GpsSensors -> "${item.latitud};${item.longitud}"
                    is ActivityItem.Photo -> item.rutaArchivo
                    is ActivityItem.Video -> item.rutaArchivo
                    is ActivityItem.Audio -> item.rutaArchivo
                }
                csvBuilder.append("${item.timestamp},$type,$detail\n")
            }
            
            val file = fileStorage.saveCsvExport(csvBuilder.toString())
            onResult(file)
        }
    }

    /**
     * Factory obligatorio para inyectar los repositorios globales desde tu Lab04EqApp
     */
    class Factory(
        private val gpsRepository: GpsRepository,
        private val mediaRepository: MediaRepository,
        private val audioRepository: AudioRepository,
        private val fileStorage: FileStorageManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(gpsRepository, mediaRepository, audioRepository, fileStorage) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
        }
    }
}
