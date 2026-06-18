package com.lab.lab04eq

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.lab.lab04eq.data.local.AppDatabase
import com.lab.lab04eq.data.local.FileStorageManager
import com.lab.lab04eq.data.repository.AudioRepository
import com.lab.lab04eq.data.repository.GpsRepository
import com.lab.lab04eq.data.repository.MediaRepository
import com.lab.lab04eq.data.session.SessionManager

class Lab04EqApp : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var gpsRepository: GpsRepository
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var fileStorage: FileStorageManager
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var audioRepository: AudioRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.obtenerBaseDatos(this)
        gpsRepository = GpsRepository(database.gpsGoogleDao(), database.gpsSensorsDao())
        sessionManager = SessionManager(this)

        fileStorage = FileStorageManager(this)
        mediaRepository = MediaRepository(database.mediaDao(), fileStorage)
        audioRepository = AudioRepository(database.audioDao(), fileStorage)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
