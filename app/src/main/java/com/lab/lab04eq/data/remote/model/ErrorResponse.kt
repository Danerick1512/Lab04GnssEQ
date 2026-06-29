package com.lab.lab04eq.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String
)
