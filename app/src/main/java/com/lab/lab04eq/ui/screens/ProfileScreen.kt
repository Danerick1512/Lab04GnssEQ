package com.lab.lab04eq.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lab.lab04eq.Lab04EqApp
import com.lab.lab04eq.data.local.entity.MediaType
import com.lab.lab04eq.model.ActivityItem
import com.lab.lab04eq.ui.viewmodel.SessionViewModel
import com.lab.lab04eq.ui.viewmodel.HistoryViewModel
import com.lab.lab04eq.ui.viewmodel.SyncViewModel
import com.lab.lab04eq.ui.viewmodel.GpsViewModel
import com.lab.lab04eq.data.remote.RetrofitClient
import com.lab.lab04eq.data.remote.NetworkConstants
import com.lab.lab04eq.data.remote.model.GeoEventResponse
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    sessionVm: SessionViewModel,
    gpsVm: GpsViewModel,
    onLogout: () -> Unit,
    username: String? = null
) {
    val app = LocalContext.current.applicationContext as Lab04EqApp
    val historyVm: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(app.gpsRepository, app.mediaRepository, app.audioRepository, app.fileStorage)
    )
    val syncVm: SyncViewModel = viewModel(
        factory = SyncViewModel.Factory(app, app.gpsRepository, app.mediaRepository, app.audioRepository, app.sessionManager)
    )

    var viewState by remember { mutableStateOf<ProfileViewState>(ProfileViewState.Menu) }
    val nombreDisplay = username ?: "Quispe Huari Erick Daniel"

    when (viewState) {
        ProfileViewState.Menu          -> ProfileMenu(
            username                = nombreDisplay,
            onLogout                = onLogout,
            onNavigateToProfile      = { viewState = ProfileViewState.MyProfile },
            onNavigateToLocal        = { viewState = ProfileViewState.LocalRecords },
            onNavigateToAll          = { viewState = ProfileViewState.AllRecords },
            onNavigateToSync         = { viewState = ProfileViewState.Sync },
            onNavigateToNotifications = { viewState = ProfileViewState.Notifications }
        )
        ProfileViewState.MyProfile     -> MyProfileScreen(username = nombreDisplay, sessionVm = sessionVm, onBack = { viewState = ProfileViewState.Menu })
        ProfileViewState.LocalRecords  -> RecordsExplorerScreen(title = "Registros locales", allowedSource = RecordsSource.LOCAL, historyVm = historyVm, sessionVm = sessionVm, onBack = { viewState = ProfileViewState.Menu })
        ProfileViewState.AllRecords    -> RecordsExplorerScreen(title = "Todos los registros", allowedSource = RecordsSource.ALL, historyVm = historyVm, sessionVm = sessionVm, onBack = { viewState = ProfileViewState.Menu })
        ProfileViewState.Sync          -> NestedScreen(title = "Sincronización",  onBack = { viewState = ProfileViewState.Menu }) { SyncScreen(syncVm) }
        ProfileViewState.Notifications -> NestedScreen(title = "Notificaciones",  onBack = { viewState = ProfileViewState.Menu }) { NotificationsScreen(syncVm) }
    }
}

private sealed class ProfileViewState {
    object Menu : ProfileViewState()
    object MyProfile : ProfileViewState()
    object LocalRecords : ProfileViewState()
    object AllRecords : ProfileViewState()
    object Sync : ProfileViewState()
    object Notifications : ProfileViewState()
}

@Composable
private fun ProfileMenu(
    username: String?,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLocal: () -> Unit,
    onNavigateToAll: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = username ?: "Usuario", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        MenuOption(Icons.Default.Person,              "Mi Perfil",          "Metadatos y configuración de tema",         onNavigateToProfile)
        Spacer(modifier = Modifier.height(12.dp))
        MenuOption(Icons.Default.History,             "Registros locales",  "Datos almacenados en este dispositivo",     onNavigateToLocal)
        Spacer(modifier = Modifier.height(12.dp))
        MenuOption(Icons.AutoMirrored.Filled.List,    "Todos los registros","Explorador local + nube (API)",             onNavigateToAll)
        Spacer(modifier = Modifier.height(12.dp))
        MenuOption(Icons.Default.CloudSync,           "Sincronización",     "Subir registros al servidor remoto",        onNavigateToSync)
        Spacer(modifier = Modifier.height(12.dp))
        MenuOption(Icons.Default.Notifications,       "Notificaciones",     "Programar y gestionar notificaciones",      onNavigateToNotifications)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick  = { mostrarConfirmacion = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Cerrar sesión")
        }
    }
    if (mostrarConfirmacion) LogoutDialog(onConfirm = onLogout, onDismiss = { mostrarConfirmacion = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordsExplorerScreen(title: String, allowedSource: RecordsSource, historyVm: HistoryViewModel, sessionVm: SessionViewModel, onBack: () -> Unit) {
    val items by historyVm.historyItems.collectAsStateWithLifecycle()

    var selectedTab  by remember { mutableIntStateOf(0) }
    val tabs         = listOf("Todos", "GNSS", "Fotos", "Videos", "Audios")
    var sourceFilter by remember { mutableStateOf(if (allowedSource == RecordsSource.ALL) RecordsSource.ALL else RecordsSource.LOCAL) }
    var remoteRecords by remember { mutableStateOf<List<GeoEventResponse>>(emptyList()) }
    var isLoadingRemote by remember { mutableStateOf(false) }
    var detailItem   by remember { mutableStateOf<ActivityItem?>(null) }

    val app = LocalContext.current.applicationContext as Lab04EqApp

    LaunchedEffect(sourceFilter) {
        if (sourceFilter != RecordsSource.LOCAL) {
            isLoadingRemote = true
            try {
                val userId = sessionVm.userId.value // O sessionManager.userId.first()
                val token = app.sessionManager.accessToken.first()
                val authHeader = if (token != null) "Bearer $token" else null

                val response = RetrofitClient.apiService.listGeoEventsORM(
                    NetworkConstants.PROJECT_SLUG,
                    authHeader,
                    userId = userId,
                    limit = 20
                )
                if (response.isSuccessful) {
                    remoteRecords = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Error silent
            } finally {
                isLoadingRemote = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Cerrar") }
        }

        if (allowedSource == RecordsSource.ALL) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = sourceFilter == RecordsSource.ALL,
                    onClick  = { sourceFilter = RecordsSource.ALL },
                    shape    = SegmentedButtonDefaults.itemShape(0, 3),
                    icon     = { Icon(Icons.AutoMirrored.Filled.List, null, Modifier.size(16.dp)) }
                ) { Text("Todo",  style = MaterialTheme.typography.labelSmall) }
                SegmentedButton(
                    selected = sourceFilter == RecordsSource.LOCAL,
                    onClick  = { sourceFilter = RecordsSource.LOCAL },
                    shape    = SegmentedButtonDefaults.itemShape(1, 3),
                    icon     = { Icon(Icons.Default.Storage, null, Modifier.size(16.dp)) }
                ) { Text("Local", style = MaterialTheme.typography.labelSmall) }
                SegmentedButton(
                    selected = sourceFilter == RecordsSource.REMOTE,
                    onClick  = { sourceFilter = RecordsSource.REMOTE },
                    shape    = SegmentedButtonDefaults.itemShape(2, 3),
                    icon     = { Icon(Icons.Default.Cloud, null, Modifier.size(16.dp)) }
                ) { Text("Nube",  style = MaterialTheme.typography.labelSmall) }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            tabs.forEachIndexed { index, t ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(t) })
            }
        }

        val filteredItems = remember(selectedTab, sourceFilter, items, remoteRecords) {
            val mappedRemote = remoteRecords.map { res ->
                val ts = try {
                    Instant.parse(res.recordedAt).toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
                ActivityItem.GpsGoogle(
                    id = res.id.toLong(),
                    timestamp = ts,
                    latitud = res.latitude,
                    longitud = res.longitude,
                    isRemote = true
                )
            }

            val combined = when (sourceFilter) {
                RecordsSource.LOCAL  -> items.filter { !it.isRemote }
                RecordsSource.REMOTE -> mappedRemote
                RecordsSource.ALL    -> items.filter { !it.isRemote } + mappedRemote
            }

            val filtered = when (selectedTab) {
                0    -> combined
                1    -> combined.filter { it is ActivityItem.GpsGoogle || it is ActivityItem.GpsSensors }
                2    -> combined.filter { it is ActivityItem.Photo }
                3    -> combined.filter { it is ActivityItem.Video }
                4    -> combined.filter { it is ActivityItem.Audio }
                else -> combined
            }
            filtered.sortedByDescending { it.timestamp }
        }

        LazyColumn(
            modifier            = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredItems) { item ->
                ActivityRow(item, onClick = { detailItem = item })
            }
        }
    }

    if (detailItem != null) {
        ActivityDetailDialog(item = detailItem!!, onDismiss = { detailItem = null })
    }
}

@Composable
private fun NestedScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Volver") }
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()
        content()
    }
}

@Composable
private fun MenuOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun MyProfileScreen(username: String?, sessionVm: SessionViewModel, onBack: () -> Unit) {
    val isDarkModePref by sessionVm.isDarkMode.collectAsStateWithLifecycle()
    val userId         by sessionVm.userId.collectAsStateWithLifecycle()
    val isDark         = isDarkModePref ?: isSystemInDarkTheme()
    val context        = LocalContext.current
    val androidId      = android.provider.Settings.Secure.getString(
        context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
    )

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Mi Perfil", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        ProfileMetadataItem("Username",         username ?: "N/A")
        ProfileMetadataItem("User ID (UUID)",   userId ?: "Cargando...")
        ProfileMetadataItem("Rol",              "Administrador / Operador")
        ProfileMetadataItem("Directorio Local", context.filesDir.absolutePath)

        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Modo Noche", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isDarkModePref == null) "Siguiendo al sistema"
                        else if (isDark) "Activado" else "Desactivado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = isDark, onCheckedChange = { sessionVm.setDarkMode(it) })
        }
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        ProfileMetadataItem("Dispositivo",      "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        ProfileMetadataItem("Android Version",  android.os.Build.VERSION.RELEASE)
        ProfileMetadataItem("API Level",        android.os.Build.VERSION.SDK_INT.toString())
        ProfileMetadataItem("Android ID",       androidId ?: "N/A")

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}

@Composable
private fun ProfileMetadataItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ActivityRow(item: ActivityItem, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }
    val icon = when(item) {
        is ActivityItem.GpsGoogle -> Icons.Default.LocationOn
        is ActivityItem.GpsSensors -> Icons.Default.LocationOn
        is ActivityItem.Photo -> Icons.Default.PhotoCamera
        is ActivityItem.Video -> Icons.Default.Videocam
        is ActivityItem.Audio -> Icons.Default.AudioFile
    }
    val label = when(item) {
        is ActivityItem.GpsGoogle -> "GNSS Google"
        is ActivityItem.GpsSensors -> "GNSS Sensor"
        is ActivityItem.Photo -> "Foto"
        is ActivityItem.Video -> "Video"
        is ActivityItem.Audio -> "Audio"
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint = if (item.isRemote) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = label, style = MaterialTheme.typography.titleSmall)
                    if (item.isRemote) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick  = {},
                            label    = { Text("Cloud", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                Text(dateFormat.format(Date(item.timestamp)), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun ActivityDetailDialog(item: ActivityItem, onDismiss: () -> Unit) {
    val context    = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle de Registro") },
        text  = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Fecha:  ${dateFormat.format(Date(item.timestamp))}")
                Text("Origen: ${if (item.isRemote) "Servidor Externo" else "Memoria del Dispositivo"}")
                Spacer(modifier = Modifier.height(8.dp))

                when (item) {
                    is ActivityItem.GpsGoogle -> {
                        Text("Lat: ${item.latitud}")
                        Text("Lon: ${item.longitud}")
                    }
                    is ActivityItem.GpsSensors -> {
                        Text("Lat: ${item.latitud}")
                        Text("Lon: ${item.longitud}")
                    }
                    is ActivityItem.Photo -> {
                        if (!item.isRemote) {
                            AsyncImage(
                                model = File(item.rutaArchivo),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Imagen en la nube")
                        }
                    }
                    is ActivityItem.Video -> {
                        Text("Video: ${item.rutaArchivo}")
                    }
                    is ActivityItem.Audio -> {
                        Text("Audio: ${item.rutaArchivo}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("¿Confirmar cierre de sesión?") },
        text    = { Text("Volverás a la pantalla de login. Tus datos locales se conservan.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Sí, cerrar sesión", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

enum class RecordsSource { LOCAL, REMOTE, ALL }
