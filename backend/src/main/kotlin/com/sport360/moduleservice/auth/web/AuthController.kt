package com.sport360.moduleservice.auth.web

import com.sport360.moduleservice.auth.service.AuthService
import com.sport360.moduleservice.auth.service.PasswordService
import com.sport360.moduleservice.auth.service.TokenService
import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.security.CookieService
import com.sport360.moduleservice.security.CurrentUserService
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val passwordService: PasswordService,
    private val tokenService: TokenService,
    private val currentUserService: CurrentUserService,
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest, response: HttpServletResponse): ApiResponse<LoginResponse> =
        ApiResponse.ok(authService.login(request.email, request.password, response))

    @PostMapping("/mfa/verify")
    fun verifyMfa(@Valid @RequestBody request: MfaVerifyRequest, response: HttpServletResponse): ApiResponse<LoginResponse> =
        ApiResponse.ok(authService.verifyMfa(request.mfaToken, request.code, response))

    @PostMapping("/mfa/resend")
    fun resendMfa(@Valid @RequestBody request: MfaResendRequest): ApiResponse<MessageResponse> {
        authService.resendMfa(request.mfaToken)
        return ApiResponse.ok(MessageResponse("Verification code sent"))
    }

    @PostMapping("/refresh-token")
    fun refresh(
        @CookieValue(name = CookieService.REFRESH_COOKIE, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): ApiResponse<MessageResponse> {
        tokenService.rotate(refreshToken, response)
        return ApiResponse.ok(MessageResponse("Token refreshed"))
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = CookieService.REFRESH_COOKIE, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): ApiResponse<MessageResponse> {
        authService.logout(refreshToken, response)
        return ApiResponse.ok(MessageResponse("Logged out"))
    }

    @GetMapping("/me")
    fun me(): ApiResponse<UserProfileResponse> =
        ApiResponse.ok(authService.me(currentUserService.currentUserId()))

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ApiResponse<MessageResponse> {
        passwordService.forgotPassword(request.email)
        return ApiResponse.ok(MessageResponse("If the email exists, a reset link has been sent"))
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ApiResponse<MessageResponse> {
        passwordService.resetPassword(request.token, request.newPassword)
        return ApiResponse.ok(MessageResponse("Password has been reset"))
    }

    @PostMapping("/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        @CookieValue(name = CookieService.REFRESH_COOKIE, required = false) refreshToken: String?,
    ): ApiResponse<MessageResponse> {
        passwordService.changePassword(
            currentUserService.currentUserId(),
            request.currentPassword,
            request.newPassword,
            refreshToken,
        )
        return ApiResponse.ok(MessageResponse("Password changed"))
    }
}
