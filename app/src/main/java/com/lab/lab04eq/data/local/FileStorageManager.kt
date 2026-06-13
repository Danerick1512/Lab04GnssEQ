package com.lab.lab04eq.data.local

import android.content.Context
import java.io.File

class FileStorageManager(private val context: Context) {

    // Inicializa y crea los directorios si no existen en el almacenamiento privado de lab04eq
    private val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
    private val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
    private val audiosDir = File(context.filesDir, "audios").apply { mkdirs() }

    // Genera un archivo único basado en el tiempo actual para Foto (.jpg)
    fun newPhotoFile(): File =
        File(photosDir, "photo_${System.currentTimeMillis()}.jpg")

    // Genera un archivo único para Video (.mp4)
    fun newVideoFile(): File =
        File(videosDir, "video_${System.currentTimeMillis()}.mp4")

    // Genera un archivo único para Audio (.m4a)
    fun newAudioFile(extension: String = "m4a"): File =
        File(audiosDir, "audio_${System.currentTimeMillis()}.$extension")

    // Elimina un archivo físico del almacenamiento del teléfono
    fun deleteFile(path: String): Boolean =
        File(path).takeIf { it.exists() }?.delete() ?: false

    // Retorna el tamaño del archivo en bytes
    fun fileSize(path: String): Long = File(path).length()
}