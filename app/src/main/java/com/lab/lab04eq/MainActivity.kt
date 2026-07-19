package com.lab.lab04eq

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.messaging.FirebaseMessaging
import com.lab.lab04eq.ui.navigation.AppNavigation
import com.lab.lab04eq.ui.theme.Lab04eqTheme
import com.lab.lab04eq.ui.viewmodel.GpsViewModel
import com.lab.lab04eq.ui.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener el contenedor global central del proceso
        val app = application as Lab04EqApp

        // Instanciar ViewModels pasando dependencias explícitamente
        val gpsViewModel     = GpsViewModel(app.gpsRepository)
        val sessionViewModel = SessionViewModel(app.sessionManager)

        setContent {
            val isDarkModePref by sessionViewModel.isDarkMode.collectAsStateWithLifecycle()
            val usarModoOscuro = isDarkModePref ?: isSystemInDarkTheme()

            // Launcher para solicitar el permiso de notificaciones de forma moderna en Compose
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Aquí podrías manejar el resultado si fuera necesario
            }

            // Solicitamos el permiso al iniciar la app (solo en Android 13+)
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // Nos suscribimos al tema global para recibir notificaciones masivas
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            }

            Lab04eqTheme(darkTheme = usarModoOscuro, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        gpsViewModel     = gpsViewModel,
                        sessionViewModel = sessionViewModel
                    )
                }
            }
        }
    }
}