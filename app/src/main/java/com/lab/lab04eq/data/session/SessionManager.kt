package com.lab.lab04eq.data.session

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {

    private companion object {
        val KEY_IS_LOGGED_IN  = booleanPreferencesKey("is_logged_in")
        val KEY_USERNAME      = stringPreferencesKey("username")
        val KEY_USER_ID       = stringPreferencesKey("user_id")
        val KEY_ACCESS_TOKEN  = stringPreferencesKey("access_token")
        val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val KEY_PROJECT_SLUG  = stringPreferencesKey("project_slug")
        val KEY_DARK_MODE     = booleanPreferencesKey("dark_mode")
    }

    val isLoggedIn: Flow<Boolean> = context.sessionDataStore.data
        .map { it[KEY_IS_LOGGED_IN] ?: false }

    val currentUsername: Flow<String?> = context.sessionDataStore.data
        .map { it[KEY_USERNAME] }

    val userId: Flow<String?> = context.sessionDataStore.data
        .map { it[KEY_USER_ID] }

    val accessToken: Flow<String?> = context.sessionDataStore.data
        .map { it[KEY_ACCESS_TOKEN] }

    val refreshToken: Flow<String?> = context.sessionDataStore.data
        .map { it[KEY_REFRESH_TOKEN] }

    val projectSlug: Flow<String> = context.sessionDataStore.data
        .map { it[KEY_PROJECT_SLUG] ?: "c13200175" }

    val isDarkMode: Flow<Boolean?> = context.sessionDataStore.data
        .map { it[KEY_DARK_MODE] }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.sessionDataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setProjectSlug(slug: String) {
        context.sessionDataStore.edit { it[KEY_PROJECT_SLUG] = slug }
    }

    suspend fun login(username: String, accessToken: String, refreshToken: String, userId: String? = null) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN]  = true
            prefs[KEY_USERNAME]      = username
            prefs[KEY_ACCESS_TOKEN]  = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            if (userId != null) prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN]  = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun logout() {
        context.sessionDataStore.edit { prefs ->
            val currentTheme = prefs[KEY_DARK_MODE]
            val currentSlug = prefs[KEY_PROJECT_SLUG]
            prefs.clear()
            if (currentTheme != null) prefs[KEY_DARK_MODE] = currentTheme
            if (currentSlug != null) prefs[KEY_PROJECT_SLUG] = currentSlug
        }
    }
}
