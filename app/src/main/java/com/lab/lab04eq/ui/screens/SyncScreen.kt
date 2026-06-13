package com.lab.lab04eq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lab.lab04eq.ui.viewmodel.SyncViewModel

@Composable
fun SyncScreen(syncViewModel: SyncViewModel) {
    val conteos by syncViewModel.syncCounts.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Consolidado de Datos (Sincronización)", style = MaterialTheme.typography.headlineSmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Resumen Total del Almacenamiento Local:", style = MaterialTheme.typography.titleLarge)
                Divider()
                Text("🛰️ Coordenadas Google GPS: ${conteos.gpsGoogle}")
                Text("🎛️ Coordenadas Sensores Internos: ${conteos.gpsSensors}")
                Text("📸 Fotografías Capturadas: ${conteos.photos}")
                Text("📹 Fragmentos de Video: ${conteos.videos}")
                Text("🎙️ Notas de Audio Registradas: ${conteos.audios}")
                Divider()
                Text(
                    text = "📦 Gran Total de Entidades: ${conteos.total}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = { /* Simulación de envío a un servidor REST de la guía */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔄 Forzar Sincronización a la Nube")
        }
    }
}