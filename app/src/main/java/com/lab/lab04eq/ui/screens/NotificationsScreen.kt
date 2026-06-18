package com.lab.lab04eq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lab.lab04eq.ui.viewmodel.SyncViewModel

@Composable
fun NotificationsScreen(syncViewModel: SyncViewModel) {
    var titulo by remember { mutableStateOf("Laboratorio 5") }
    var mensaje by remember { mutableStateOf("¡Alerta en segundo plano completada con éxito!") }
    val lastWorkId by syncViewModel.lastWorkId.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Programación de Alertas", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título de Notificación") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = mensaje,
            onValueChange = { mensaje = it },
            label = { Text("Mensaje de la Alerta") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                syncViewModel.scheduleDelayedNotification(titulo, mensaje)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("🔔 Agendar Notificación (10s delay)")
        }

        if (lastWorkId != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⏳ Tarea programada (ID: ${lastWorkId.toString().take(8)}...)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { syncViewModel.cancelDelayedNotification() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("❌ Cancelar Alerta")
                    }
                }
            }
        }
    }
}
