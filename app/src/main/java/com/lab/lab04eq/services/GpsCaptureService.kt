package com.lab.lab04eq.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lab.lab04eq.Lab04EqApp
import com.lab.lab04eq.data.local.entity.GpsGoogleEntity
import com.lab.lab04eq.data.local.entity.GpsSensorsEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.*

class GpsCaptureService : Service() {

    companion object {
        private const val INTERVAL_MS        = 5_000L
        private const val SENSOR_TIMEOUT_MS  = 5_000L
        private const val NOTIFICATION_ID    = 1001
        private const val CHANNEL_ID         = "gps_capture_channel"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var captureJob: Job? = null

    private val gpsRepo by lazy { (application as Lab04EqApp).gpsRepository }

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(applicationContext)
    }

    private val locationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (captureJob == null) {
            captureJob = scope.launch {
                while (isActive) {
                    performCaptures()
                    delay(INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private suspend fun performCaptures() {
        val now = System.currentTimeMillis()

        // 1. Captura vía Google Fused Location Provider
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    loc?.let {
                        scope.launch {
                            gpsRepo.saveGooglePoint(GpsGoogleEntity(
                                latitud = it.latitude,
                                longitud = it.longitude,
                                altitud = it.altitude,
                                precision = it.accuracy,
                                timestamp = now
                            ))
                        }
                    }
                }
        } catch (e: Exception) { e.printStackTrace() }

        // 2. Captura vía Chip de Hardware (LocationManager Legacy Adaptado)
        try {
            val sensorLoc = withTimeoutOrNull(SENSOR_TIMEOUT_MS) { getRawGpsLocation() }
            gpsRepo.saveSensorsPoint(GpsSensorsEntity(
                latitud = sensorLoc?.latitude ?: 0.0,
                longitud = sensorLoc?.longitude ?: 0.0,
                altitud = sensorLoc?.altitude ?: 0.0,
                satelitesEnUso = 0, // No disponible directamente así
                timestamp = now
            ))
        } catch (e: Exception) { e.printStackTrace() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getRawGpsLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val listener = android.location.LocationListener { location ->
            if (continuation.isActive) continuation.resume(location, null)
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, mainLooper)
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWith(Result.failure(e))
        }
        continuation.invokeOnCancellation {
            locationManager.removeUpdates(listener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureJob?.cancel()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Captura GNSS Activa")
            .setContentText("Registrando coordenadas en paralelo cada 10s...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Servicio GNSS", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}