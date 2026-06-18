package com.lab.lab04eq.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.lab.lab04eq.data.local.entity.MediaEntity
import com.lab.lab04eq.ui.viewmodel.MediaViewModel
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaScreen(mediaViewModel: MediaViewModel) {
    val contexto = LocalContext.current
    val fotoCount by mediaViewModel.photoCount.collectAsState()
    val videoCount by mediaViewModel.videoCount.collectAsState()
    val mediaItems by mediaViewModel.mediaItems.collectAsState()

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempVideoFile by remember { mutableStateOf<File?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { exito ->
        if (exito && tempPhotoFile != null) mediaViewModel.registerPhoto(tempPhotoFile!!)
    }

    val videoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CaptureVideo()) { _ ->
        if (tempVideoFile != null && tempVideoFile!!.exists() && tempVideoFile!!.length() > 0) {
            mediaViewModel.registerVideo(tempVideoFile!!)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Captura Multimedia", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📸 Fotos", style = MaterialTheme.typography.labelMedium)
                    Text("$fotoCount", style = MaterialTheme.typography.headlineMedium)
                }
                VerticalDivider(modifier = Modifier.height(40.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📹 Videos", style = MaterialTheme.typography.labelMedium)
                    Text("$videoCount", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!permissionState.allPermissionsGranted) {
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }, modifier = Modifier.fillMaxWidth()) {
                Text("Conceder Permisos de Cámara")
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val file = mediaViewModel.preparePhotoFile()
                    tempPhotoFile = file
                    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", file)
                    photoLauncher.launch(uri)
                }, modifier = Modifier.weight(1f)) { Text("Tomar Foto") }

                Button(onClick = {
                    val file = mediaViewModel.prepareVideoFile()
                    tempVideoFile = file
                    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", file)
                    videoLauncher.launch(uri)
                }, modifier = Modifier.weight(1f)) { Text("Grabar Video") }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Capturas Recientes:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(mediaItems) { item ->
                MediaItemRow(item, onDelete = { mediaViewModel.deleteMedia(item) })
            }
        }
    }
}

@Composable
fun MediaItemRow(item: MediaEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = File(item.rutaArchivo),
                contentDescription = null,
                modifier = Modifier.size(60.dp).padding(4.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(if (item.tipo == "PHOTO") "Foto 📸" else "Video 📹", style = MaterialTheme.typography.labelLarge)
                Text(item.rutaArchivo.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
