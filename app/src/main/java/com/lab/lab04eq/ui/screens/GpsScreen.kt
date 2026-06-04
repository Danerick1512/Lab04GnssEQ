package com.lab.lab04eq.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lab.lab04eq.ui.viewmodel.ComparativeGpsRecord
import com.lab.lab04eq.ui.viewmodel.GpsViewModel
import com.lab.lab04eq.services.GpsCaptureService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GpsScreen(viewModel: GpsViewModel) {
    val context = LocalContext.current
    val permisos = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    val estadoPermisos = rememberMultiplePermissionsState(permissions = permisos)
    var capturando by remember { mutableStateOf(false) }

    val googlePoints by viewModel.googlePoints.collectAsStateWithLifecycle()
    val sensorsPoints by viewModel.sensorsPoints.collectAsStateWithLifecycle()
    val history by viewModel.comparativeHistory.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!estadoPermisos.allPermissionsGranted) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requieren permisos de ubicación para continuar con el laboratorio.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { estadoPermisos.launchMultiplePermissionRequest() }) { Text("Conceder permisos") }
                }
            }
            return@Column
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    capturando = !capturando
                    val intent = Intent(context, GpsCaptureService::class.java)
                    if (capturando) context.startForegroundService(intent) else context.stopService(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (capturando) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(if (capturando) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (capturando) "Detener" else "Capturar (5s)")
            }

            OutlinedButton(
                onClick = { viewModel.clearAll() },
                modifier = Modifier.weight(0.6f)
            ) {
                Text("Limpiar")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Google FLP")
                    Text("${googlePoints.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sensores GNSS")
                    Text("${sensorsPoints.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(items = history, key = { it.timestamp }) { record ->
                ComparativeCaptureCard(record, dateFormat)
            }
        }
    }
}

@Composable
fun ComparativeCaptureCard(record: ComparativeGpsRecord, dateFormat: SimpleDateFormat) {
    val distance = remember(record) {
        if (record.google != null && record.sensors != null) {
            haversine(
                record.google.latitud, record.google.longitud,
                record.sensors.latitud, record.sensors.longitud
            )
        } else null
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Instante: ${dateFormat.format(Date(record.timestamp))}", fontWeight = FontWeight.Bold)
                distance?.let {
                    Text("Δ: ${"%.2f".format(it)} m", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GOOGLE FLP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    record.google?.let {
                        Text("Lat: ${it.latitud}")
                        Text("Lon: ${it.longitud}")
                    } ?: Text("Buscando...")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("CHIP SENSORES", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    record.sensors?.let {
                        Text("Lat: ${it.latitud}")
                        Text("Lon: ${it.longitud}")
                        Text("Satélites: ${it.satelitesEnUso}", style = MaterialTheme.typography.bodySmall)
                    } ?: Text("Buscando...")
                }
            }
        }
    }
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371e3
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
