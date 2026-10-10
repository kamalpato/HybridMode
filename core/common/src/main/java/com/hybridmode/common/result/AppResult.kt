package com.hybridmode.common.result

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val failure: AppFailure) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(block: (T) -> Unit): AppResult<T> {
        if (this is Success) block(data)
        return this
    }

    inline fun onError(block: (AppFailure) -> Unit): AppResult<T> {
        if (this is Error) block(failure)
        return this
    }
}

sealed class AppFailure(open val message: String, open val cause: Throwable? = null) {
    data class Network(
        override val message: String = "Network unavailable",
        override val cause: Throwable? = null,
    ) : AppFailure(message, cause)

    data class Unauthorized(
        override val message: String = "Session expired. Please sign in again.",
        override val cause: Throwable? = null,
    ) : AppFailure(message, cause)

    data class Validation(
        override val message: String,
        override val cause: Throwable? = null,
    ) : AppFailure(message, cause)

    data class NotFound(
        override val message: String = "Resource not found",
        override val cause: Throwable? = null,
    ) : AppFailure(message, cause)

    data class Unknown(
        override val message: String = "Something went wrong",
        override val cause: Throwable? = null,
    ) : AppFailure(message, cause)
}
