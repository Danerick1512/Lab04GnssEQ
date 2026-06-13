package com.lab.lab04eq.model

/**
 * Clase sellada que agrupa jerárquicamente todos los tipos de eventos
 * capturados en los laboratorios 4 y 5 para su renderizado unificado en la interfaz.
 */
sealed class ActivityItem {
    abstract val id: Long
    abstract val timestamp: Long

    // 1. Elemento para representar geolocalizaciones por la API de Google Play Services
    data class GpsGoogle(
        override val id: Long,
        override val timestamp: Long,
        val latitud: Double,
        val longitud: Double,
        val proveedor: String = "Google Play Services"
    ) : ActivityItem()

    // 2. Elemento para representar geolocalizaciones crudas leídas por el hardware de sensores
    data class GpsSensors(
        override val id: Long,
        override val timestamp: Long,
        val latitud: Double,
        val longitud: Double,
        val proveedor: String = "Internal Sensors"
    ) : ActivityItem()

    // 3. Elemento para representar capturas fotográficas de la cámara
    data class Photo(
        override val id: Long,
        override val timestamp: Long,
        val rutaArchivo: String
    ) : ActivityItem()

    // 4. Elemento para representar grabaciones de video de la cámara
    data class Video(
        override val id: Long,
        override val timestamp: Long,
        val rutaArchivo: String,
        val duracionMs: Long
    ) : ActivityItem()

    // 5. Elemento para representar grabaciones por nota de voz desde el micrófono
    data class Audio(
        override val id: Long,
        override val timestamp: Long,
        val rutaArchivo: String,
        val duracionMs: Long,
        val formato: String
    ) : ActivityItem()
}