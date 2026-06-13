package com.lab.lab04eq

import android.app.Application
import com.lab.lab04eq.data.local.AppDatabase
import com.lab.lab04eq.data.local.FileStorageManager
import com.lab.lab04eq.data.repository.AudioRepository
import com.lab.lab04eq.data.repository.GpsRepository
import com.lab.lab04eq.data.repository.MediaRepository
import com.lab.lab04eq.data.session.SessionManager

class Lab04EqApp : Application() {

    // Contenedores globales para la inyección de dependencias manual (Conservados de tu Lab 4)
    lateinit var database: AppDatabase
        private set

    lateinit var gpsRepository: GpsRepository
        private set

    lateinit var sessionManager: SessionManager
        private set

    // NUEVOS: Contenedores globales agregados para los componentes del Lab 5
    lateinit var fileStorage: FileStorageManager
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var audioRepository: AudioRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Inicialización única de instancias globales (Lab 4)
        database = AppDatabase.obtenerBaseDatos(this)
        gpsRepository = GpsRepository(database.gpsGoogleDao(), database.gpsSensorsDao())
        sessionManager = SessionManager(this)

        // NUEVOS: Inicialización de la infraestructura multimedia del Lab 5
        fileStorage = FileStorageManager(this)
        mediaRepository = MediaRepository(database.mediaDao(), fileStorage)
        audioRepository = AudioRepository(database.audioDao(), fileStorage)
    }
}