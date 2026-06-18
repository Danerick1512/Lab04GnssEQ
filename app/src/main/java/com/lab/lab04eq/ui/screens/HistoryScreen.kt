package com.lab.lab04eq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lab.lab04eq.model.ActivityItem
import com.lab.lab04eq.ui.viewmodel.HistoryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel) {
    val items by historyViewModel.historyItems.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial Unificado",
                style = MaterialTheme.typography.headlineSmall
            )
            
            IconButton(onClick = { 
                historyViewModel.exportToCsv { file ->
                    // En un caso real aquí usaríamos un Intent para compartir el archivo
                }
            }) {
                Text("📊 CSV", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay registros capturados.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
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
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Mostrar miniatura si es Multimedia
            if (item is ActivityItem.Photo || item is ActivityItem.Video) {
                val path = if (item is ActivityItem.Photo) item.rutaArchivo else (item as ActivityItem.Video).rutaArchivo
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(end = 12.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (item) {
                            is ActivityItem.GpsGoogle -> "🛰️ Google GPS"
                            is ActivityItem.GpsSensors -> "🎛️ Sensores"
                            is ActivityItem.Photo -> "📸 Foto"
                            is ActivityItem.Video -> "📹 Video"
                            is ActivityItem.Audio -> "🎙️ Audio"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(text = fechaLegible, style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (item) {
                    is ActivityItem.GpsGoogle -> Text("Lat: ${item.latitud}, Lon: ${item.longitud}", style = MaterialTheme.typography.bodySmall)
                    is ActivityItem.GpsSensors -> Text("Lat: ${item.latitud}, Lon: ${item.longitud}", style = MaterialTheme.typography.bodySmall)
                    is ActivityItem.Photo -> Text("Img: ${item.rutaArchivo.substringAfterLast("/")}", style = MaterialTheme.typography.bodySmall)
                    is ActivityItem.Video -> Text("Vid: ${item.rutaArchivo.substringAfterLast("/")}", style = MaterialTheme.typography.bodySmall)
                    is ActivityItem.Audio -> Text("Aud: ${item.rutaArchivo.substringAfterLast("/")}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
