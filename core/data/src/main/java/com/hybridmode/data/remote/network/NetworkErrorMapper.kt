package com.hybridmode.data.remote.network

import com.hybridmode.common.result.AppFailure
import com.hybridmode.data.remote.dto.StrapiErrorEnvelope
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkErrorMapper {
    fun map(throwable: Throwable, json: Json): AppFailure = when (throwable) {
        is UnknownHostException,
        is SocketTimeoutException,
        is IOException,
        -> AppFailure.Network(
            message = "Network unavailable",
            cause = throwable,
        )

        is HttpException -> {
            val body = throwable.response()?.errorBody()?.string()
            val apiMessage = body?.let { parseStrapiMessage(it, json) }
            when (throwable.code()) {
                401 -> AppFailure.Unauthorized(
                    message = friendlyAuthMessage(apiMessage)
                        ?: "Your session expired. Please sign in again.",
                    cause = throwable,
                )
                403 -> AppFailure.Unauthorized(
                    message = "You don’t have permission to do this. " +
                        "In Strapi Admin → Settings → Roles → Authenticated, enable Upload.",
                    cause = throwable,
                )
                404 -> AppFailure.NotFound(
                    message = apiMessage ?: "Resource not found",
                    cause = throwable,
                )
                400, 409, 422 -> AppFailure.Validation(
                    message = apiMessage ?: "Invalid request",
                    cause = throwable,
                )
                else -> AppFailure.Unknown(
                    message = apiMessage ?: "Server error (${throwable.code()})",
                    cause = throwable,
                )
            }
        }

        else -> AppFailure.Unknown(
            message = throwable.message ?: "Something went wrong",
            cause = throwable,
        )
    }

    private fun parseStrapiMessage(body: String, json: Json): String? =
        runCatching {
            val envelope = json.decodeFromString<StrapiErrorEnvelope>(body)
            envelope.error?.message ?: envelope.message
        }.getOrNull()

    private fun friendlyAuthMessage(apiMessage: String?): String? {
        if (apiMessage.isNullOrBlank()) return null
        val lower = apiMessage.lowercase()
        return if (
            lower.contains("invalid credential") ||
            lower.contains("missing or invalid")
        ) {
            "Session expired. Logout, then login again."
        } else {
            apiMessage
        }
    }
}
