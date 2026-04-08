package com.sport360.moduleservice.common

/** Base for all business exceptions mapped to the API error envelope. */
open class ApiException(
    val errorCode: ErrorCode,
    override val message: String,
    val fieldErrors: List<FieldErrorDto>? = null,
) : RuntimeException(message)

class ValidationException(message: String, fieldErrors: List<FieldErrorDto>? = null) :
    ApiException(ErrorCode.VALIDATION_ERROR, message, fieldErrors)

class UnauthorizedException(message: String = "Authentication required") :
    ApiException(ErrorCode.UNAUTHORIZED, message)

class ForbiddenException(message: String = "Access denied") :
    ApiException(ErrorCode.FORBIDDEN, message)

class NotFoundException(message: String = "Resource not found") :
    ApiException(ErrorCode.NOT_FOUND, message)

class ConflictException(message: String) :
    ApiException(ErrorCode.CONFLICT, message)

class RateLimitedException(message: String = "Too many requests") :
    ApiException(ErrorCode.RATE_LIMITED, message)
