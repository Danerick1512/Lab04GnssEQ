package com.lab.lab04eq.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.lab.lab04eq.ui.viewmodel.AudioViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioScreen(audioViewModel: AudioViewModel) {
    val isRecording by audioViewModel.isRecording.collectAsState()
    val elapsedSeconds by audioViewModel.elapsedSeconds.collectAsState()
    val audioCount by audioViewModel.count.collectAsState()

    val recordAudioPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Grabadora de Audio", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estadísticas de Micrófono:", style = MaterialTheme.typography.titleMedium)
                Text("🎙️ Notas de voz guardadas: $audioCount")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!recordAudioPermissionState.status.isGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requiere permiso de micrófono para grabar audio.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { recordAudioPermissionState.launchPermissionRequest() }) {
                        Text("Conceder Permiso")
                    }
                }
            }
        } else {
            if (isRecording) {
                Text(
                    text = "🔴 Grabando: ${elapsedSeconds}s",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineMedium
                )
                Button(
                    onClick = { audioViewModel.stopRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("⏹️ Detener Grabación")
                }
            } else {
                Text("Grabadora Lista", style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = { audioViewModel.startRecording() },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("🎙️ Iniciar Captura de Voz")
                }
            }
        }
    }
}
