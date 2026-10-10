package com.hybridmode.data.remote.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class KotlinxSerializationConverterFactory private constructor(
    private val json: Json,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        if (type == Unit::class.java || type == Void::class.java) return null
        val serializer = json.serializersModule.serializer(type)
        return Converter<ResponseBody, Any> { body ->
            val payload = body.string()
            if (payload.isBlank()) {
                error("Empty response body for $type")
            }
            json.decodeFromString(serializer, payload) as Any
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? {
        if (type == Unit::class.java || type == Void::class.java) return null
        if (type is Class<*> && RequestBody::class.java.isAssignableFrom(type)) return null

        val serializer = json.serializersModule.serializer(type) as KSerializer<Any>
        return Converter<Any, RequestBody> { value ->
            json.encodeToString(serializer, value).toRequestBody(CONTENT_TYPE)
        }
    }

    companion object {
        private val CONTENT_TYPE = "application/json".toMediaType()

        fun create(json: Json): KotlinxSerializationConverterFactory =
            KotlinxSerializationConverterFactory(json)
    }
}
