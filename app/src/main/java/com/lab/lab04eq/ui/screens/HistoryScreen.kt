package com.lab.lab04eq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lab.lab04eq.model.ActivityItem
import com.lab.lab04eq.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel) {
    val items by historyViewModel.historyItems.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Historial de Actividades Unificado",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay registros capturados en este laboratorio.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Sintaxis corregida para el iterador de elementos con llave única en Compose
                items(
                    items = items,
                    key = { item -> "${item.javaClass.simpleName}_${item.id}" }
                ) { item ->
                    HistoryCard(item = item)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: ActivityItem) {
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val fechaLegible = formatoFecha.format(Date(item.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item) {
                is ActivityItem.GpsGoogle -> MaterialTheme.colorScheme.surfaceVariant
                is ActivityItem.GpsSensors -> MaterialTheme.colorScheme.primaryContainer
                is ActivityItem.Photo -> MaterialTheme.colorScheme.secondaryContainer
                is ActivityItem.Video -> MaterialTheme.colorScheme.tertiaryContainer
                is ActivityItem.Audio -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (item) {
                        is ActivityItem.GpsGoogle -> "🛰️ Ubicación (Google GPS)"
                        is ActivityItem.GpsSensors -> "🎛️ Ubicación (Hardware Sensores)"
                        is ActivityItem.Photo -> "📸 Captura Fotográfica"
                        is ActivityItem.Video -> "📹 Grabación de Video"
                        is ActivityItem.Audio -> "🎙️ Nota de Audio"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = fechaLegible, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Renderizado de la data según la sealed class del modelo
            when (item) {
                is ActivityItem.GpsGoogle -> {
                    Text("Latitud: ${item.latitud}")
                    Text("Longitud: ${item.longitud}")
                }
                is ActivityItem.GpsSensors -> {
                    Text("Latitud: ${item.latitud}")
                    Text("Longitud: ${item.longitud}")
                }
                is ActivityItem.Photo -> {
                    Text("Ruta: ${item.rutaArchivo.substringAfterLast("/")}")
                }
                is ActivityItem.Video -> {
                    Text("Ruta: ${item.rutaArchivo.substringAfterLast("/")}")
                }
                is ActivityItem.Audio -> {
                    Text("Formato: ${item.formato}")
                    Text("Ruta: ${item.rutaArchivo.substringAfterLast("/")}")
                }
            }
        }
    }
}