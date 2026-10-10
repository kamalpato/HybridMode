package com.hybridmode.data.remote.mock

import com.hybridmode.data.BuildConfig
import com.hybridmode.data.remote.dto.StrapiAuthResponseDto
import com.hybridmode.data.remote.dto.StrapiErrorBody
import com.hybridmode.data.remote.dto.StrapiErrorEnvelope
import com.hybridmode.data.remote.dto.StrapiLoginRequestDto
import com.hybridmode.data.remote.dto.StrapiRegisterRequestDto
import com.hybridmode.data.remote.dto.StrapiUploadFileDto
import com.hybridmode.data.remote.dto.StrapiUserDto
import com.hybridmode.domain.auth.AuthTokens
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAuthInterceptor @Inject constructor(
    private val json: Json,
) : Interceptor {

    private data class Account(
        val id: Long,
        val email: String,
        val password: String,
        val username: String,
        val createdAt: String,
    )

    private data class IssuedJwt(
        val token: String,
        val userId: Long,
        val expiresAt: Long,
    )

    private val accounts = ConcurrentHashMap<String, Account>()
    private val jwtIndex = ConcurrentHashMap<String, IssuedJwt>()
    private val refreshIndex = ConcurrentHashMap<String, IssuedJwt>()
    private val nextUserId = AtomicLong(2)
    private val nextFileId = AtomicLong(1)

    init {
        val demo = Account(
            id = 1L,
            email = "demo@hybridmode.app",
            password = "password123",
            username = "demo",
            createdAt = "2024-01-01T00:00:00.000Z",
        )
        accounts[demo.email] = demo
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val now = System.currentTimeMillis()

        return when {
            path.endsWith("/api/auth/local/register") && request.method == "POST" -> {
                val rawBody = request.body?.let { bufferBody(it) }.orEmpty()
                val body = json.decodeFromString<StrapiRegisterRequestDto>(rawBody.ifBlank { "{}" })
                val email = body.email.lowercase()
                if (accounts.containsKey(email)) {
                    error(request, 400, "Email or Username are already taken")
                } else {
                    val account = Account(
                        id = nextUserId.getAndIncrement(),
                        email = email,
                        password = body.password,
                        username = body.username,
                        createdAt = Instant.ofEpochMilli(now).toString(),
                    )
                    accounts[email] = account
                    success(request, issueAuth(account, now))
                }
            }

            path.endsWith("/api/auth/local") && request.method == "POST" -> {
                val rawBody = request.body?.let { bufferBody(it) }.orEmpty()
                val body = json.decodeFromString<StrapiLoginRequestDto>(rawBody.ifBlank { "{}" })
                val identifier = body.identifier.lowercase()
                val account = accounts[identifier]
                    ?: accounts.values.firstOrNull { it.username.equals(identifier, true) }
                if (account == null || account.password != body.password) {
                    error(request, 400, "Invalid identifier or password")
                } else {
                    success(request, issueAuth(account, now))
                }
            }

            path.endsWith("/api/auth/refresh") && request.method == "POST" -> {
                val rawBody = request.body?.let { bufferBody(it) }.orEmpty()
                val refresh = runCatching {
                    json.decodeFromString<Map<String, String>>(rawBody.ifBlank { "{}" })["refreshToken"]
                }.getOrNull().orEmpty()
                val issued = refreshIndex.remove(refresh)
                val account = issued?.let { accById(it.userId) }
                if (issued == null || account == null) {
                    error(request, 401, "Invalid refresh token")
                } else {
                    jwtIndex.remove(issued.token)
                    success(request, issueAuth(account, now))
                }
            }

            path.endsWith("/api/auth/logout") && request.method == "POST" -> {
                val issued = resolveJwt(request.header("Authorization"), now)
                val rawBody = request.body?.let { bufferBody(it) }.orEmpty()
                val refresh = runCatching {
                    json.decodeFromString<Map<String, String>>(rawBody.ifBlank { "{}" })["refreshToken"]
                }.getOrNull().orEmpty()
                if (issued != null) {
                    jwtIndex.remove(issued.token)
                    refreshIndex.entries
                        .filter { it.value.userId == issued.userId }
                        .forEach { refreshIndex.remove(it.key) }
                } else if (refresh.isNotBlank()) {
                    refreshIndex.remove(refresh)?.let { jwtIndex.remove(it.token) }
                }
                success(request, """{"ok":true}""")
            }

            path.endsWith("/api/users/me") && request.method == "GET" -> {
                val issued = resolveJwt(request.header("Authorization"), now)
                val account = issued?.let { accById(it.userId) }
                when {
                    issued == null || account == null -> error(request, 401, "Unauthorized")
                    else -> success(request, json.encodeToString(toUserDto(account)))
                }
            }

            path.endsWith("/api/upload") && request.method == "POST" -> {
                val issued = resolveJwt(request.header("Authorization"), now)
                when {
                    issued == null -> error(request, 401, "Unauthorized")
                    else -> {
                        request.body?.writeTo(okio.Buffer())
                        val fileId = nextFileId.getAndIncrement()
                        val dto = StrapiUploadFileDto(
                            id = fileId,
                            name = "upload-$fileId.jpg",
                            url = "/uploads/mock-$fileId.jpg",
                            mime = "image/jpeg",
                            alternativeText = "uploaded",
                            caption = "",
                            createdAt = Instant.ofEpochMilli(now).toString(),
                        )
                        success(request, json.encodeToString(listOf(dto)))
                    }
                }
            }

            else -> error(request, 404, "Not found: $path")
        }
    }

    private fun issueAuth(account: Account, now: Long): String {
        val token = "jwt_${UUID.randomUUID()}"
        val refresh = "refresh_${UUID.randomUUID()}"
        val issued = IssuedJwt(
            token = token,
            userId = account.id,
            expiresAt = now + AuthTokens.MAX_ACCESS_TOKEN_TTL_MS,
        )
        jwtIndex[token] = issued
        refreshIndex[refresh] = issued
        return json.encodeToString(
            StrapiAuthResponseDto(
                jwt = token,
                user = toUserDto(account),
                refreshToken = refresh,
            )
        )
    }

    private fun toUserDto(account: Account) = StrapiUserDto(
        id = account.id,
        username = account.username,
        email = account.email,
        createdAt = account.createdAt,
        confirmed = true,
        blocked = false,
        provider = "local",
    )

    private fun resolveJwt(header: String?, now: Long): IssuedJwt? {
        val token = header?.removePrefix("Bearer ")?.trim().orEmpty()
        if (token.isBlank()) return null
        val issued = jwtIndex[token] ?: return null
        if (now >= issued.expiresAt) {
            jwtIndex.remove(token)
            return null
        }
        return issued
    }

    private fun accById(id: Long): Account? = accounts.values.firstOrNull { it.id == id }

    private fun bufferBody(body: okhttp3.RequestBody): String {
        val buffer = okio.Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun success(request: okhttp3.Request, jsonBody: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonBody.toResponseBody(JSON))
            .build()

    private fun error(request: okhttp3.Request, code: Int, message: String): Response {
        val payload = json.encodeToString(
            StrapiErrorEnvelope(error = StrapiErrorBody(status = code, message = message))
        )
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(payload.toResponseBody(JSON))
            .build()
    }

    companion object {
        val USE_MOCK_AUTH: Boolean get() = BuildConfig.USE_MOCK_BACKEND
        private val JSON = "application/json".toMediaType()
    }
}
