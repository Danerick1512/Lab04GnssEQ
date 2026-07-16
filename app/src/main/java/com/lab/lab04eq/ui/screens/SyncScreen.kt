package com.lab.lab04eq.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lab.lab04eq.ui.viewmodel.SyncViewModel
import com.lab.lab04eq.data.remote.model.GeoEventResponse

@Composable
fun SyncScreen(syncViewModel: SyncViewModel) {
    val context = LocalContext.current
    val conteos by syncViewModel.syncCounts.collectAsStateWithLifecycle()
    val isSyncing by syncViewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by syncViewModel.syncMessage.collectAsStateWithLifecycle()
    val syncProgress by syncViewModel.syncProgress.collectAsStateWithLifecycle()
    val cloudRecords by syncViewModel.cloudRecords.collectAsStateWithLifecycle()
    val isLoadingCloud by syncViewModel.isLoadingCloud.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        syncViewModel.refreshCloudData()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
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
                syncViewModel.sync { success ->
                    if (success) {
                        Toast.makeText(context, "Sincronización finalizada", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isSyncing && conteos.total > 0,
            modifier = Modifier.fillMaxWidth().height(56.dp)
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
                Text("🔄 Sincronizar ahora")
            }
        }
        
        if (isSyncing) {
            LinearProgressIndicator(
                progress = { syncProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = syncMessage ?: "Sincronizando...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        } else if (syncMessage != null) {
            Text(
                text = syncMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (syncMessage!!.contains("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        // ── Sección de Datos en la Nube ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Datos en la nube (Servidor)", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { syncViewModel.refreshCloudData() }) {
                Text("Actualizar")
            }
        }

        if (isLoadingCloud) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (cloudRecords.isEmpty() && !isLoadingCloud) {
            Text(
                "No hay datos registrados en el servidor para este usuario.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            cloudRecords.forEach { record ->
                CloudRecordCard(record)
            }
        }
    }
}

@Composable
private fun CloudRecordCard(record: GeoEventResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ID: ${record.id} • ${record.eventType ?: "GPS"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${record.latitude}, ${record.longitude}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Registrado: ${record.recordedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
