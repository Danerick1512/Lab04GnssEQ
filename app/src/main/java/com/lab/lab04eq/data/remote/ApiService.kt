package com.lab.lab04eq.data.remote

import com.lab.lab04eq.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("{projectSlug}/auth/register")
    suspend fun register(
        @Path("projectSlug") projectSlug: String,
        @Body request: RegisterRequest
    ): Response<Unit>

    @POST("{projectSlug}/auth/login")
    suspend fun login(
        @Path("projectSlug") projectSlug: String,
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @POST("{projectSlug}/auth/google")
    suspend fun loginWithGoogle(
        @Path("projectSlug") projectSlug: String,
        @Body request: GoogleLoginRequest
    ): Response<TokenResponse>

    @POST("{projectSlug}/auth/refresh-token")
    suspend fun refreshToken(
        @Path("projectSlug") projectSlug: String,
        @Body request: RefreshTokenRequest
    ): Response<TokenResponse>

    @GET("{projectSlug}/auth/me")
    suspend fun me(
        @Path("projectSlug") projectSlug: String,
        @Header("Authorization") token: String
    ): Response<UserMeResponse>

    // ── ENDPOINTS DE SINCRONIZACIÓN (Autorización automática vía Interceptor) ──

    @POST("{projectSlug}/geo-events-orm/")
    suspend fun createGeoEventORM(
        @Path("projectSlug") projectSlug: String,
        @Header("Authorization") token: String?,
        @Body request: GeoEventRequest
    ): Response<GeoEventResponse>

    @GET("{projectSlug}/geo-events-orm/")
    suspend fun listGeoEventsORM(
        @Path("projectSlug") projectSlug: String,
        @Header("Authorization") token: String?,
        @Query("user_id") userId: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<GeoEventResponse>>

    @POST("{projectSlug}/sync/gps")
    suspend fun syncGps(
        @Path("projectSlug") projectSlug: String,
        @Body request: List<GpsSyncRequest>
    ): Response<Unit>

    @Multipart
    @POST("{projectSlug}/sync/media")
    suspend fun uploadMedia(
        @Path("projectSlug") projectSlug: String,
        @Part file: MultipartBody.Part,
        @Part("tipo") tipo: RequestBody,
        @Part("timestamp") timestamp: RequestBody
    ): Response<Unit>
}
