package com.sport360.moduleservice.auth.web

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

data class MfaVerifyRequest(
    @field:NotBlank val mfaToken: String,
    @field:NotBlank
    @field:Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
    val code: String,
)

data class MfaResendRequest(
    @field:NotBlank val mfaToken: String,
)

data class ForgotPasswordRequest(
    @field:Email @field:NotBlank val email: String,
)

data class ResetPasswordRequest(
    @field:NotBlank val token: String,
    @field:NotBlank
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String,
)

data class ChangePasswordRequest(
    @field:NotBlank val currentPassword: String,
    @field:NotBlank
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val newPassword: String,
)
