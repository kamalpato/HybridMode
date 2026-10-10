package com.hybridmode.data.mapper

import com.hybridmode.data.BuildConfig
import com.hybridmode.data.remote.dto.StrapiFileInfoDto
import com.hybridmode.data.remote.dto.StrapiUploadFileDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun StrapiUploadFileDto.absoluteUrl(): String {
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    val base = BuildConfig.BASE_URL.trimEnd('/')
    val path = if (url.startsWith("/")) url else "/$url"
    return base + path
}

fun buildStrapiFileInfoJson(
    json: Json,
    title: String,
    description: String,
): String = json.encodeToString(
    StrapiFileInfoDto(
        name = title,
        alternativeText = title,
        caption = description,
    )
)
