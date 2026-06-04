package com.lab.lab04eq

import android.app.Application
import com.lab.lab04eq.data.local.AppDatabase
import com.lab.lab04eq.data.repository.GpsRepository
import com.lab.lab04eq.data.session.SessionManager

class Lab04EqApp : Application() {

    // Contenedores globales para la inyección de dependencias manual (Tema L3 del lab)
    lateinit var database: AppDatabase
        private set

    lateinit var gpsRepository: GpsRepository
        private set

    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Inicialización única de instancias globales
        database = AppDatabase.obtenerBaseDatos(this)
        gpsRepository = GpsRepository(database.gpsGoogleDao(), database.gpsSensorsDao())
        sessionManager = SessionManager(this)
    }
}
