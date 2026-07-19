package com.lab.lab04eq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.lab.lab04eq.data.remote.NetworkConstants
import com.lab.lab04eq.data.remote.RetrofitClient
import com.lab.lab04eq.data.remote.model.*
import com.lab.lab04eq.data.session.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

class SessionViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val isLoggedIn = sessionManager.isLoggedIn.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = false
    )

    val username = sessionManager.currentUsername.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = null
    )

    val userId = sessionManager.userId.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = null
    )

    val isDarkMode = sessionManager.isDarkMode.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = null
    )

    val notificationsEnabled = sessionManager.notificationsEnabled.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = true
    )

    val currentSlug: StateFlow<String> = sessionManager.projectSlug.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkConstants.PROJECT_SLUG
    )

    fun updateSlug(newSlug: String) {
        viewModelScope.launch {
            sessionManager.setProjectSlug(newSlug)
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    projectSlug = currentSlug.value,
                    request     = LoginRequest(
                        email    = email.trim(),
                        password = password.trim(),
                        deviceId = sessionManager.getDeviceId()
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Recuperamos el user_id de /me
                    var finalUserId: String? = null
                    val meResponse = RetrofitClient.apiService.me(
                        currentSlug.value,
                        "Bearer ${body.accessToken}"
                    )
                    if (meResponse.isSuccessful) {
                        finalUserId = meResponse.body()?.user?.userId
                    }

                    sessionManager.login(email.trim(), body.accessToken, body.refreshToken, finalUserId)
                    fetchAndSyncToken()
                    onResult(true, null)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    onResult(false, errorMsg ?: "Credenciales incorrectas")
                }
            } catch (e: Exception) {
                onResult(false, "Error de red: ${e.localizedMessage}")
            }
        }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    projectSlug = currentSlug.value,
                    request     = RegisterRequest(email.trim(), password.trim())
                )
                if (response.isSuccessful) {
                    onResult(true, null)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    onResult(false, errorMsg ?: "Error al registrar")
                }
            } catch (e: Exception) {
                onResult(false, "Error de red: ${e.localizedMessage}")
            }
        }
    }

    fun loginWithGoogle(googleToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.loginWithGoogle(
                    projectSlug = currentSlug.value,
                    request     = GoogleLoginRequest(
                        token    = googleToken,
                        deviceId = sessionManager.getDeviceId()
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Recuperamos el user_id de /me
                    var finalUserId: String? = null
                    val meResponse = RetrofitClient.apiService.me(
                        currentSlug.value,
                        "Bearer ${body.accessToken}"
                    )
                    if (meResponse.isSuccessful) {
                        finalUserId = meResponse.body()?.user?.userId
                    }

                    sessionManager.login("Google User", body.accessToken, body.refreshToken, finalUserId)
                    fetchAndSyncToken()
                    onResult(true, null)
                } else {
                    val errorMsg = parseError(response.errorBody()?.string())
                    onResult(false, errorMsg ?: "Error al autenticar con Google")
                }
            } catch (e: Exception) {
                onResult(false, "Error de red: ${e.localizedMessage}")
            }
        }
    }

    private fun parseError(errorBody: String?): String? {
        return try {
            errorBody?.let {
                val errorResponse = json.decodeFromString<ErrorResponse>(it)
                errorResponse.message
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { sessionManager.setDarkMode(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setNotificationsEnabled(enabled)
            if (enabled) {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users")
            }
        }
    }

    private fun fetchAndSyncToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                syncFcmToken(token)
            } catch (e: Exception) {
                // Error al obtener token de Firebase
            }
        }
    }

    fun syncFcmToken(fcmToken: String) {
        viewModelScope.launch {
            try {
                val token = sessionManager.accessToken.firstOrNull()
                val uId = sessionManager.userId.firstOrNull()
                val uName = sessionManager.currentUsername.firstOrNull()

                if (token != null) {
                    RetrofitClient.apiService.updateFcmToken(
                        projectSlug = currentSlug.value,
                        token = "Bearer $token",
                        request = DeviceTokenRequest(
                            userId = uId,
                            userName = uName,
                            fcmToken = fcmToken,
                            deviceId = sessionManager.getDeviceId()
                        )
                    )
                }
            } catch (e: Exception) {
                // Manejar error de red
            }
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.logout() }
    }

    class Factory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionViewModel(sessionManager) as T
    }
}
