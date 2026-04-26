package com.rajatt7z.retailx.network

import retrofit2.http.Body
import retrofit2.http.POST

data class NotificationRequest(
    val receiver: String,
    val message: String
)

data class CloudinaryResponse(
    val url: String,
    val public_id: String
)

interface NotificationService {
    @POST("api/v1/send-sms")
    suspend fun sendNotification(@Body request: NotificationRequest): retrofit2.Response<Unit>

    @retrofit2.http.Multipart
    @POST("api/v1/images/upload")
    suspend fun uploadImage(
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): CloudinaryResponse
}
