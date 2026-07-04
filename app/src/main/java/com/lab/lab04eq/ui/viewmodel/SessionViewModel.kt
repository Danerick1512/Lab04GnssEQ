package com.lab.lab04eq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lab.lab04eq.data.remote.NetworkConstants
import com.lab.lab04eq.data.remote.RetrofitClient
import com.lab.lab04eq.data.remote.model.*
import com.lab.lab04eq.data.session.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    val isDarkMode = sessionManager.isDarkMode.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = null
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
                    sessionManager.login(email.trim(), body.accessToken, body.refreshToken)
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
                    sessionManager.login("Google User", body.accessToken, body.refreshToken)
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

    fun logout() {
        viewModelScope.launch { sessionManager.logout() }
    }

    class Factory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionViewModel(sessionManager) as T
    }
}
