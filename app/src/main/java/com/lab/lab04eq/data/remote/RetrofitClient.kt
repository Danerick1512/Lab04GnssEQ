package com.lab.lab04eq.data.remote

import com.lab.lab04eq.data.session.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object RetrofitClient {
    private var sessionManager: SessionManager? = null

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues  = true
        encodeDefaults     = true
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Inicializa el cliente con el SessionManager para manejar tokens automáticamente.
     */
    fun init(manager: SessionManager) {
        this.sessionManager = manager
    }

    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking { sessionManager?.accessToken?.firstOrNull() }
        val request = chain.request().newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        chain.proceed(request)
    }

    private val authenticator = Authenticator { _, response ->
        if (response.code == 401) {
            val newTokenResponse = runBlocking {
                try {
                    val refreshToken = sessionManager?.refreshToken?.firstOrNull()
                    if (refreshToken != null) {
                        val resp = apiService.refreshToken(
                            NetworkConstants.PROJECT_SLUG,
                            com.lab.lab04eq.data.remote.model.RefreshTokenRequest(
                                refreshToken,
                                sessionManager?.getDeviceId() ?: ""
                            )
                        )
                        if (resp.isSuccessful && resp.body() != null) {
                            val body = resp.body()!!
                            sessionManager?.updateTokens(body.accessToken, body.refreshToken)
                            body.accessToken
                        } else null
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            return@Authenticator if (newTokenResponse != null) {
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newTokenResponse")
                    .build()
            } else null
        }
        null
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(NetworkConstants.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
