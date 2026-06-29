package com.lab.lab04eq.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lab.lab04eq.data.remote.NetworkConstants
import com.lab.lab04eq.data.remote.RetrofitClient
import com.lab.lab04eq.data.remote.model.*
import com.lab.lab04eq.data.session.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    val isLoggedIn = sessionManager.isLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val username = sessionManager.currentUsername.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val isDarkMode = sessionManager.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    NetworkConstants.PROJECT_SLUG,
                    LoginRequest(
                        email = email.trim(),
                        password = password.trim(),
                        deviceId = sessionManager.getDeviceId()
                    )
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        sessionManager.login(email.trim(), it.accessToken, it.refreshToken)
                        onResult(true)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Login failed", e)
            }
            onResult(false)
        }
    }

    fun register(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    NetworkConstants.PROJECT_SLUG,
                    RegisterRequest(email = email.trim(), password = password.trim())
                )
                onResult(response.isSuccessful)
                return@launch
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Register failed", e)
            }
            onResult(false)
        }
    }

    fun loginWithGoogle(googleToken: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.loginWithGoogle(
                    NetworkConstants.PROJECT_SLUG,
                    GoogleLoginRequest(token = googleToken, deviceId = sessionManager.getDeviceId())
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        sessionManager.login("google_user", it.accessToken, it.refreshToken)
                        onResult(true)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Google login failed", e)
            }
            onResult(false)
        }
    }

    fun refreshSession(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val refreshToken = sessionManager.refreshToken.firstOrNull()
                if (refreshToken.isNullOrBlank()) {
                    onResult(false)
                    return@launch
                }

                val response = RetrofitClient.apiService.refreshToken(
                    NetworkConstants.PROJECT_SLUG,
                    RefreshTokenRequest(
                        refreshToken = refreshToken,
                        deviceId = sessionManager.getDeviceId()
                    )
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        sessionManager.updateTokens(it.accessToken, it.refreshToken)
                        onResult(true)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Refresh token failed", e)
            }
            onResult(false)
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
