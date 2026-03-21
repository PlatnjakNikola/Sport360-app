package com.sport360.moduleservice.common

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Maps business and framework exceptions to the standard error envelope. */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ResponseEntity<ApiResponse<Nothing>> =
        build(ex.errorCode, ex.message, ex.fieldErrors)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val details = ex.bindingResult.fieldErrors.map {
            FieldErrorDto(it.field, it.defaultMessage ?: "Invalid value")
        }
        return build(ErrorCode.VALIDATION_ERROR, "Validation failed", details)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> =
        build(ErrorCode.FORBIDDEN, "Access denied", null)

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception", ex)
        return build(ErrorCode.INTERNAL_ERROR, "Internal server error", null)
    }

    private fun build(
        code: ErrorCode,
        message: String,
        details: List<FieldErrorDto>?,
    ): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(code.status).body(ApiResponse.failure(ApiError(code.name, message, details)))
}
