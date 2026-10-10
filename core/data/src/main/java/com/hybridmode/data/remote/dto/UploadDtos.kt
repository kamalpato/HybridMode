package com.hybridmode.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StrapiUploadFileDto(
    val id: Long,
    val name: String? = null,
    val url: String,
    val mime: String? = null,
    val size: Double? = null,
    @SerialName("alternativeText") val alternativeText: String? = null,
    val caption: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class StrapiFileInfoDto(
    val name: String? = null,
    val alternativeText: String? = null,
    val caption: String? = null,
)
