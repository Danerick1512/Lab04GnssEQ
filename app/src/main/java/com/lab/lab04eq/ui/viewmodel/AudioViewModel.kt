package com.lab.lab04eq.ui.viewmodel

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lab.lab04eq.data.local.FileStorageManager
import com.lab.lab04eq.data.local.entity.AudioEntity
import com.lab.lab04eq.data.repository.AudioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AudioViewModel(
    private val context: Context,
    private val audioRepository: AudioRepository,
    private val fileStorage: FileStorageManager
) : ViewModel() {

    val audios = audioRepository.allAudios.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val count = audioRepository.count.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0
    )

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    // Nueva funcionalidad: Amplitud para barra de nivel
    private val _amplitude = MutableStateFlow(0f)
    val amplitude = _amplitude.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L
    private var timerJob: Job? = null

    fun startRecording(): Boolean {
        if (_isRecording.value) return false

        return try {
            val file = fileStorage.newAudioFile(extension = "m4a")
            currentFile = file

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            startTimeMs = System.currentTimeMillis()
            _isRecording.value = true
            _elapsedSeconds.value = 0

            timerJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(100) // Actualización más frecuente para la barra de volumen
                    _elapsedSeconds.value = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
                    
                    // Obtener amplitud (0 a 32767) y normalizar
                    val maxAmp = recorder?.maxAmplitude ?: 0
                    _amplitude.value = maxAmp.toFloat() / 32767f
                }
            }
            Log.d("AudioViewModel", "Grabación iniciada: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("AudioViewModel", "Error al iniciar grabación", e)
            cleanup()
            false
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return

        val file = currentFile
        val durationMs = System.currentTimeMillis() - startTimeMs

        try {
            recorder?.apply {
                stop()
                release()
            }
            if (file != null && file.exists() && durationMs >= 1000L) {
                viewModelScope.launch {
                    audioRepository.registerAudio(
                        filePath = file.absolutePath,
                        durationMs = durationMs,
                        format = "AAC"
                    )
                }
                Log.d("AudioViewModel", "Grabación guardada: ${file.absolutePath} ($durationMs ms)")
            } else {
                Log.w("AudioViewModel", "Grabación descartada (muy corta o archivo inexistente)")
                file?.takeIf { it.exists() }?.delete()
            }
        } catch (e: Exception) {
            Log.e("AudioViewModel", "Error al detener grabación", e)
            file?.takeIf { it.exists() }?.delete()
        } finally {
            cleanup()
        }
    }

    fun delete(item: AudioEntity) {
        viewModelScope.launch { audioRepository.delete(item) }
    }

    private fun cleanup() {
        timerJob?.cancel()
        timerJob = null
        recorder = null
        currentFile = null
        _isRecording.value = false
        _elapsedSeconds.value = 0
        _amplitude.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) {
            try {
                recorder?.apply { stop(); release() }
            } catch (_: Exception) { }
            currentFile?.takeIf { it.exists() }?.delete()
        }
    }

    class Factory(
        private val context: Context,
        private val audioRepository: AudioRepository,
        private val fileStorage: FileStorageManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AudioViewModel(context, audioRepository, fileStorage) as T
    }
}
