package com.lab.lab04eq.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DelayedNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "LAB05_NOTIF_CHANNEL"
        const val NOTIFICATION_ID = 5001
    }

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Laboratorio 5"
        val message = inputData.getString("message") ?: "Tarea programada ejecutada con éxito"

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal de notificaciones obligatorio para Android 8.0 (API 26) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones Programadas Lab05",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para alertas diferidas en segundo plano del laboratorio 5"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Construcción de la notificación nativa con compatibilidad hacia atrás
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono del sistema por defecto
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        // Disparar la notificación al panel superior del celular
        notificationManager.notify(NOTIFICATION_ID, notification)

        return Result.success()
    }
}