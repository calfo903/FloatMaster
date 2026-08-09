package com.floatmaster.util

import java.time.Instant
import java.util.UUID

/**
 * WHY: Sealed Result prevents leaking exceptions to UI / throwing; forces explicit success/failure handling.
 * Never expose internal exception messages to clients — map to structured AppError.
 */
sealed interface Result<out T> {
    data class Success<T>(val value: T, val timestamp: Instant = Instant.now()) : Result<T>
    data class Failure(val error: AppError) : Result<Nothing>

    fun isSuccess(): Boolean = this is Success
    fun fold(onSuccess: (T) -> Unit, onFailure: (AppError) -> Unit) {
        when (this) { is Success -> onSuccess(value); is Failure -> onFailure(error) }
    }
    fun getOrNull(): T? = (this as? Success)?.value
}

/**
 * WHY: Value class for ID prevents mixing WindowId with UserId/other UUID strings; zero runtime overhead.
 */
@JvmInline
value class WindowId(val value: UUID) {
    companion object { fun generate() = WindowId(UUID.randomUUID()); fun fromString(s: String) = WindowId(UUID.fromString(s)) }
    override fun toString(): String = value.toString()
}

/**
 * WHY: Structured error envelope — consistent, actionable, never leaks stack trace.
 */
sealed interface AppError {
    val code: String
    val message: String
    val hint: String?
    val httpStatus: Int // mapped to UI: 400/409/429/500 etc.

    data class Validation(val field: String?, override val message: String, override val hint: String? = null) : AppError {
        override val code = "VALIDATION_ERROR"; override val httpStatus = 400
    }
    data class Conflict(override val message: String, override val hint: String? = null) : AppError {
        override val code = "CONFLICT"; override val httpStatus = 409
    }
    data class RateLimited(override val message: String = "Too many windows, wait a moment") : AppError {
        override val code = "RATE_LIMITED"; override val httpStatus = 429; override val hint = "Close some windows or wait 2s"
    }
    data class NotFound(override val message: String) : AppError {
        override val code = "NOT_FOUND"; override val httpStatus = 404; override val hint = "Verify window ID"
    }
    data class Security(override val message: String) : AppError {
        override val code = "SECURITY_BLOCKED"; override val httpStatus = 403; override val hint = "URL not in allowlist"
    }
    data class Internal(val cause: Throwable? = null, override val message: String = "Unable to process request") : AppError {
        override val code = "INTERNAL"; override val httpStatus = 500; override val hint = "Retry with idempotency key"
    }
}

/** WHY: Never expose internal exception messages — map to AppError */
fun Throwable.toAppError(): AppError = AppError.Internal(cause = this)

/** WHY: Extension to wrap runCatching into Result */
inline fun <T> runResult(block: () -> T): Result<T> = try { Result.Success(block()) } catch (e: Exception) { Result.Failure(e.toAppError()) }
