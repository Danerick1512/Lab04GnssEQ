package com.lab.lab04eq.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lab.lab04eq.ui.viewmodel.SyncViewModel

@Composable
fun SyncScreen(syncViewModel: SyncViewModel) {
    val context = LocalContext.current
    val conteos by syncViewModel.syncCounts.collectAsState()
    val isSyncing by syncViewModel.isSyncing.collectAsState()

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
                HorizontalDivider()
                Text("🛰️ Coordenadas Google GPS: ${conteos.gpsGoogle}")
                Text("🎛️ Coordenadas Sensores Internos: ${conteos.gpsSensors}")
                Text("📸 Fotografías Capturadas: ${conteos.photos}")
                Text("📹 Fragmentos de Video: ${conteos.videos}")
                Text("🎙️ Notas de Audio Registradas: ${conteos.audios}")
                HorizontalDivider()
                Text(
                    text = "📦 Gran Total de Entidades: ${conteos.total}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = {
                syncViewModel.forceSync { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            enabled = !isSyncing && conteos.total > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sincronizando...")
            } else {
                Text("🔄 Forzar Sincronización a la Nube")
            }
        }
        
        if (isSyncing) {
            Text(
                "Subiendo archivos a Cloudflare R2 y metadatos a MongoDB...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
