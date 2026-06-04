package com.lab.lab04eq.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lab.lab04eq.ui.screens.GpsScreen
import com.lab.lab04eq.ui.screens.ProfileScreen
import com.lab.lab04eq.ui.viewmodel.GpsViewModel
import com.lab.lab04eq.ui.viewmodel.SessionViewModel

sealed class Ruta(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    object Gps    : Ruta("gps",    "Captura GNSS", Icons.Default.Place)
    object Perfil : Ruta("perfil", "Mi Perfil",    Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(gpsViewModel: GpsViewModel, sessionViewModel: SessionViewModel) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val tabs = listOf(Ruta.Gps, Ruta.Perfil)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentDestination?.route == Ruta.Gps.ruta) "Lab 04: GNSS Dual" else "Panel del Estudiante") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val seleccionada = currentDestination?.hierarchy?.any { it.route == tab.ruta } == true
                    NavigationBarItem(
                        selected = seleccionada,
                        onClick = {
                            navController.navigate(tab.ruta) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icono, contentDescription = tab.etiqueta) },
                        label = { Text(tab.etiqueta) }
                    )
                }
            }
        }
    )
{ paddingValues ->
        NavHost(navController = navController, startDestination = Ruta.Gps.ruta, modifier = Modifier.padding(paddingValues)) {
            composable(Ruta.Gps.ruta) { GpsScreen(viewModel = gpsViewModel) }
            composable(Ruta.Perfil.ruta) { 
                ProfileScreen(
                    sessionVm = sessionViewModel, 
                    gpsVm = gpsViewModel,
                    onLogout = { sessionViewModel.logout() }
                ) 
            }
        }
    }
}