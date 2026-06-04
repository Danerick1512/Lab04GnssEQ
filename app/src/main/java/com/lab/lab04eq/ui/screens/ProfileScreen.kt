package com.lab.lab04eq.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lab.lab04eq.ui.viewmodel.SessionViewModel
import com.lab.lab04eq.ui.viewmodel.GpsViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.*

private sealed class ProfileViewState {
    object Menu       : ProfileViewState()
    object MyProfile  : ProfileViewState()
    object MyActivity : ProfileViewState()
}

@Composable
fun ProfileScreen(sessionVm: SessionViewModel, gpsVm: GpsViewModel, onLogout: () -> Unit) {
    var viewState by remember { mutableStateOf<ProfileViewState>(ProfileViewState.Menu) }
    val username by sessionVm.username.collectAsStateWithLifecycle()

    when (viewState) {
        is ProfileViewState.Menu -> ProfileMenu(
            username = username ?: "Quispe Huari Erick Daniel",
            onNavigateToProfile = { viewState = ProfileViewState.MyProfile },
            onNavigateToActivity = { viewState = ProfileViewState.MyActivity },
            onLogoutClick = onLogout
        )
        is ProfileViewState.MyProfile -> MyProfileScreen(
            sessionVm = sessionVm,
            username = username ?: "N/A",
            onBack = { viewState = ProfileViewState.Menu }
        )
        is ProfileViewState.MyActivity -> MyActivityScreen(
            gpsVm = gpsVm,
            onBack = { viewState = ProfileViewState.Menu }
        )
    }
}

@Composable
private fun ProfileMenu(username: String, onNavigateToProfile: () -> Unit, onNavigateToActivity: () -> Unit, onLogoutClick: () -> Unit) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Facultad de Ingeniería de Sistemas", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Card(onClick = onNavigateToProfile, modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Mis Datos", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("Información personal y del dispositivo físico") },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            )
        }

        Card(onClick = onNavigateToActivity, modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Historial de Actividad", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("Verificación de registros multimedia consolidados") },
                leadingContent = { Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(onClick = { mostrarDialogo = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar sesión académica")
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("¿Seguro que deseas salir?") },
            text = { Text("Tus preferencias de modo oscuro no se verán alteradas.") },
            confirmButton = { TextButton(onClick = { mostrarDialogo = false; onLogoutClick() }) { Text("Sí, salir", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MyProfileScreen(sessionVm: SessionViewModel, username: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isDarkModePref by sessionVm.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isDarkModePref ?: isSystemInDarkTheme()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Información de Sistema", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Text("Usuario Activo: $username", fontWeight = FontWeight.Medium)
        Text("Ruta del Directorio Físico:\n${context.filesDir.absolutePath}", style = MaterialTheme.typography.bodySmall)
        Text("Equipo: ${Build.MANUFACTURER.uppercase()} - ${Build.MODEL}", style = MaterialTheme.typography.bodyMedium)
        Text("Plataforma: Android ${Build.VERSION.RELEASE} (SDK API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Modo Oscuro", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("Cambiar aspecto estático de la app") },
                trailingContent = { Switch(checked = isDark, onCheckedChange = { sessionVm.setDarkMode(it) }) }
            )
        }
    }
}

@Composable
private fun MyActivityScreen(gpsVm: GpsViewModel, onBack: () -> Unit) {
    val history by gpsVm.comparativeHistory.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Historial de Actividad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        if (history.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No hay archivos multimedia registrados en este ciclo.")
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(items = history, key = { it.timestamp }) { record ->
                    ComparativeCaptureCard(record, dateFormat)
                }
            }
        }
    }
}
