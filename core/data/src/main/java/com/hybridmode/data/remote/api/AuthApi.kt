package com.hybridmode.data.remote.api

import com.hybridmode.data.remote.dto.StrapiAuthResponseDto
import com.hybridmode.data.remote.dto.StrapiLoginRequestDto
import com.hybridmode.data.remote.dto.StrapiRefreshRequestDto
import com.hybridmode.data.remote.dto.StrapiRegisterRequestDto
import com.hybridmode.data.remote.dto.StrapiUserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/local")
    suspend fun login(@Body body: StrapiLoginRequestDto): StrapiAuthResponseDto

    @POST("api/auth/local/register")
    suspend fun register(@Body body: StrapiRegisterRequestDto): StrapiAuthResponseDto

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: StrapiRefreshRequestDto): StrapiAuthResponseDto

    @GET("api/users/me")
    suspend fun me(@Header("Authorization") bearer: String): StrapiUserDto
}
