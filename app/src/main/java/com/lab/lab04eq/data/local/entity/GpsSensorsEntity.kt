package com.lab.lab04eq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_sensors_updates")
data class GpsSensorsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitud: Double,
    val longitud: Double,
    val altitud: Double,
    val satelitesEnUso: Int,
    val timestamp: Long
)