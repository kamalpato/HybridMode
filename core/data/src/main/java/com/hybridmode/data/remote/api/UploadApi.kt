package com.hybridmode.data.remote.api

import com.hybridmode.data.remote.dto.StrapiUploadFileDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadApi {
    @Multipart
    @POST("api/upload")
    suspend fun upload(
        @Header("Authorization") bearer: String,
        @Part files: MultipartBody.Part,
        @Part("fileInfo") fileInfo: RequestBody,
    ): List<StrapiUploadFileDto>
}
