package com.lab.lab04eq.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lab.lab04eq.Lab04EqApp
import com.lab.lab04eq.ui.screens.*
import com.lab.lab04eq.ui.viewmodel.*

sealed class Ruta(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    object Login   : Ruta("login",   "Login",   Icons.Default.Lock)
    object Register: Ruta("register","Registro",Icons.Default.PersonAdd)
    object Gps     : Ruta("gps",     "GNSS",    Icons.Default.MyLocation)
    object Media   : Ruta("media",   "Cámara",  Icons.Default.PhotoCamera)
    object Audio   : Ruta("audio",   "Audio",   Icons.Default.Mic)
    object Perfil  : Ruta("perfil",  "Perfil",  Icons.Default.AccountCircle)
}

@Composable
fun AppNavigation(gpsViewModel: GpsViewModel, sessionViewModel: SessionViewModel) {
    val rootNavController = rememberNavController()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            rootNavController.navigate("main") {
                popUpTo("auth") { inclusive = true }
            }
        } else {
            rootNavController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = rootNavController,
        startDestination = if (isLoggedIn) "main" else "auth"
    ) {
        navigation(startDestination = Ruta.Login.ruta, route = "auth") {
            composable(Ruta.Login.ruta) {
                LoginScreen(
                    onSubmit = { email, pass, onResult ->
                        sessionViewModel.login(email, pass, onResult)
                    },
                    onRegisterNavigate = { rootNavController.navigate(Ruta.Register.ruta) }
                )
            }
            composable(Ruta.Register.ruta) {
                RegisterScreen(
                    onBack = { rootNavController.popBackStack() },
                    onSubmit = { email, password, onResult ->
                        sessionViewModel.register(email, password, onResult)
                    }
                )
            }
        }

        composable("main") {
            MainScaffold(sessionViewModel, gpsViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(sessionViewModel: SessionViewModel, gpsViewModel: GpsViewModel) {
    val contextApp = LocalContext.current.applicationContext as Lab04EqApp
    val nav = rememberNavController()
    val currentBackStack by nav.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val username by sessionViewModel.username.collectAsStateWithLifecycle()

    val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(contextApp.mediaRepository, contextApp.fileStorage))
    val audioViewModel: AudioViewModel = viewModel(factory = AudioViewModel.Factory(contextApp, contextApp.audioRepository, contextApp.fileStorage))

    val tabs = listOf(Ruta.Gps, Ruta.Media, Ruta.Audio, Ruta.Perfil)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentDestination?.route) {
                            Ruta.Gps.ruta -> "Lab 04: GNSS Dual"
                            Ruta.Media.ruta -> "Lab 05: Captura Multimedia"
                            Ruta.Audio.ruta -> "Lab 05: Grabadora"
                            Ruta.Perfil.ruta -> "Mi Perfil"
                            else -> "DemoData"
                        }
                    )
                },
                actions = {
                    IconButton(onClick = { sessionViewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val seleccionada = currentDestination?.hierarchy?.any { it.route == tab.ruta } == true
                    NavigationBarItem(
                        selected = seleccionada,
                        alwaysShowLabel = false,
                        onClick = {
                            nav.navigate(tab.ruta) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icono, contentDescription = tab.etiqueta) },
                        label = {
                            Text(
                                text = tab.etiqueta,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Ruta.Gps.ruta,
            modifier = Modifier.padding(padding)
        ) {
            composable(Ruta.Gps.ruta) { GpsScreen(viewModel = gpsViewModel) }
            composable(Ruta.Media.ruta) { MediaScreen(mediaViewModel = mediaViewModel) }
            composable(Ruta.Audio.ruta) { AudioScreen(audioViewModel = audioViewModel) }
            composable(Ruta.Perfil.ruta) {
                ProfileScreen(
                    sessionVm = sessionViewModel,
                    gpsVm = gpsViewModel,
                    onLogout = { sessionViewModel.logout() },
                    username = username
                )
            }
        }
    }
}
