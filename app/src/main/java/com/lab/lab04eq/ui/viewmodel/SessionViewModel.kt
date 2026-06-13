package com.lab.lab04eq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lab.lab04eq.data.session.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

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

    fun login(username: String, password: String, onResult: (Boolean) -> Unit) {
        val userClean = username.trim().lowercase()
        val passClean = password.trim()

        if (userClean == "jkn" && passClean == "jkn") {
            viewModelScope.launch {
                sessionManager.login(userClean)
                onResult(true)
            }
        } else {
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
