package com.lab.lab04eq.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lab.lab04eq.data.local.dao.AudioDao
import com.lab.lab04eq.data.local.dao.GpsGoogleDao
import com.lab.lab04eq.data.local.dao.GpsSensorsDao
import com.lab.lab04eq.data.local.dao.MediaDao
import com.lab.lab04eq.data.local.entity.AudioEntity
import com.lab.lab04eq.data.local.entity.GpsGoogleEntity
import com.lab.lab04eq.data.local.entity.GpsSensorsEntity
import com.lab.lab04eq.data.local.entity.MediaEntity

@Database(
    entities = [
        GpsGoogleEntity::class,
        GpsSensorsEntity::class,
        MediaEntity::class,    // Agregado para Lab 5
        AudioEntity::class     // Agregado para Lab 5
    ],
    version = 3, // Incrementado a versión 3 para soportar las nuevas tablas
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gpsGoogleDao(): GpsGoogleDao
    abstract fun gpsSensorsDao(): GpsSensorsDao
    abstract fun mediaDao(): MediaDao    // Agregado para Lab 5
    abstract fun audioDao(): AudioDao    // Agregado para Lab 5

    companion object {
        @Volatile
        private var INSTANCIA: AppDatabase? = null

        fun obtenerBaseDatos(contexto: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDatabase::class.java,
                    "lab04eq_database" // Se mantiene tu nombre de archivo original
                )
                    .fallbackToDestructiveMigration() // Recrea las tablas limpiamente al cambiar de versión
                    .build()
                INSTANCIA = instancia
                instancia
            }
        }
    }
}