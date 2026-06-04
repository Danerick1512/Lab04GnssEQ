package com.lab.lab04eq.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lab.lab04eq.data.local.dao.GpsGoogleDao
import com.lab.lab04eq.data.local.dao.GpsSensorsDao
import com.lab.lab04eq.data.local.entity.GpsGoogleEntity
import com.lab.lab04eq.data.local.entity.GpsSensorsEntity

@Database(entities = [GpsGoogleEntity::class, GpsSensorsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gpsGoogleDao(): GpsGoogleDao
    abstract fun gpsSensorsDao(): GpsSensorsDao

    companion object {
        @Volatile
        private var INSTANCIA: AppDatabase? = null

        fun obtenerBaseDatos(contexto: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDatabase::class.java,
                    "lab04eq_database"
                ).build()
                INSTANCIA = instancia
                instancia
            }
        }
    }
}