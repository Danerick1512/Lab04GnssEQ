package com.lab.lab04eq.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class GpsSyncRequest(
    val latitud: Double,
    val longitud: Double,
    val altitud: Double,
    val precision: Float? = null,
    val provider: String,
    val timestamp: Long
)
