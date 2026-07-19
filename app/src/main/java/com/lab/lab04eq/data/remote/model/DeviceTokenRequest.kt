package com.lab.lab04eq.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRequest(
    @SerialName("user_id") val userId: String?,
    @SerialName("user_name") val userName: String?,
    @SerialName("device_id") val deviceId: String,
    @SerialName("fcm_token") val fcmToken: String
)
