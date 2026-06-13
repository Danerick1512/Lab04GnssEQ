package com.lab.lab04eq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val rutaArchivo: String,          // Ruta absoluta interna de filesDir
    val tipo: String,                // "PHOTO" o "VIDEO" (usando MediaType.name)
    val tamanoBytes: Long,
    val duracionMs: Long? = null,    // Solo para videos, null para fotos
    val anchoPx: Int? = null,
    val altoPx: Int? = null,
    val timestamp: Long
)

enum class MediaType { PHOTO, VIDEO }