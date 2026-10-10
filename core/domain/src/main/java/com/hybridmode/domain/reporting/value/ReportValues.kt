package com.hybridmode.domain.reporting.value

import com.hybridmode.domain.shared.ValueObject

@JvmInline
value class ReportId private constructor(val value: String) : ValueObject {
    companion object {
        fun create(raw: String): ReportId {
            require(raw.isNotBlank()) { "Report id required" }
            return ReportId(raw)
        }

        fun rehydrate(raw: String): ReportId = ReportId(raw)
    }

    override fun toString(): String = value
}

@JvmInline
value class ReportTitle private constructor(val value: String) : ValueObject {
    companion object {
        fun create(raw: String): ReportTitle {
            val cleaned = raw.trim()
            require(cleaned.length >= 2) { "Title must be at least 2 characters" }
            return ReportTitle(cleaned)
        }

        fun rehydrate(raw: String): ReportTitle = ReportTitle(raw)
    }

    override fun toString(): String = value
}

@JvmInline
value class ReportDescription private constructor(val value: String) : ValueObject {
    companion object {
        fun create(raw: String): ReportDescription = ReportDescription(raw.trim())
        fun rehydrate(raw: String): ReportDescription = ReportDescription(raw)
    }

    override fun toString(): String = value
}

@JvmInline
value class LocalImagePath private constructor(val value: String) : ValueObject {
    companion object {
        fun create(raw: String): LocalImagePath {
            require(raw.isNotBlank()) { "Image path required" }
            return LocalImagePath(raw)
        }

        fun rehydrate(raw: String): LocalImagePath = LocalImagePath(raw)
    }

    override fun toString(): String = value
}

@JvmInline
value class MimeType private constructor(val value: String) : ValueObject {
    companion object {
        fun create(raw: String): MimeType {
            val cleaned = raw.ifBlank { "image/jpeg" }
            return MimeType(cleaned)
        }

        fun rehydrate(raw: String): MimeType = MimeType(raw)
    }

    override fun toString(): String = value
}
