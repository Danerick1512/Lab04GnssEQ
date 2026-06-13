package com.lab.lab04eq.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.lab.lab04eq.ui.viewmodel.MediaViewModel
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaScreen(mediaViewModel: MediaViewModel) {
    val contexto = LocalContext.current
    val fotoCount by mediaViewModel.photoCount.collectAsState()
    val videoCount by mediaViewModel.videoCount.collectAsState()

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempVideoFile by remember { mutableStateOf<File?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito && tempPhotoFile != null) {
            Log.d("MediaScreen", "Foto capturada exitosamente: ${tempPhotoFile!!.absolutePath}")
            mediaViewModel.registerPhoto(tempPhotoFile!!)
        } else {
            Log.e("MediaScreen", "Fallo al capturar foto o cancelado")
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { exito ->
        // Algunos dispositivos no retornan exito=true pero el archivo se crea
        if (tempVideoFile != null && tempVideoFile!!.exists() && tempVideoFile!!.length() > 0) {
            Log.d("MediaScreen", "Video grabado exitosamente: ${tempVideoFile!!.absolutePath}")
            mediaViewModel.registerVideo(tempVideoFile!!)
        } else {
            Log.e("MediaScreen", "Fallo al grabar video o archivo vacío")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Captura Multimedia", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estadísticas de Cámara:", style = MaterialTheme.typography.titleMedium)
                Text("📸 Fotos capturadas: $fotoCount")
                Text("📹 Videos grabados: $videoCount")
            }
        }

        if (!permissionState.allPermissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requieren permisos de cámara y audio.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                        Text("Conceder Permisos")
                    }
                }
            }
        } else {
            Button(
                onClick = {
                    try {
                        val file = mediaViewModel.preparePhotoFile()
                        tempPhotoFile = file
                        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", file)
                        photoLauncher.launch(uri)
                    } catch (e: Exception) {
                        Log.e("MediaScreen", "Error al lanzar cámara para foto", e)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📸 Tomar Foto")
            }

            Button(
                onClick = {
                    try {
                        val file = mediaViewModel.prepareVideoFile()
                        tempVideoFile = file
                        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", file)
                        videoLauncher.launch(uri)
                    } catch (e: Exception) {
                        Log.e("MediaScreen", "Error al lanzar cámara para video", e)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📹 Grabar Video")
            }
        }
    }
}
