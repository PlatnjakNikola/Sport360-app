package com.sport360.moduleservice.common

import com.fasterxml.jackson.annotation.JsonInclude

/** A single field-level validation error. */
data class FieldErrorDto(val field: String, val message: String)

/** Error payload of the response envelope. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
    val details: List<FieldErrorDto>? = null,
)

/** Standard response envelope: `{success, data}` on success, `{success, error}` on failure. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun failure(error: ApiError): ApiResponse<Nothing> = ApiResponse(success = false, error = error)
    }
}
