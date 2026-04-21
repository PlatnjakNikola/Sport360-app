package com.sport360.moduleservice.auth.service

import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.auth.domain.PasswordResetToken
import com.sport360.moduleservice.auth.repository.PasswordResetTokenRepository
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.UnauthorizedException
import com.sport360.moduleservice.common.ValidationException
import com.sport360.moduleservice.config.AppProperties
import com.sport360.moduleservice.email.EmailService
import com.sport360.moduleservice.security.Tokens
import com.sport360.moduleservice.users.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/** Forgot/reset password (email link) and authenticated change-password. */
@Service
class PasswordService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
    private val tokenService: TokenService,
    private val auditService: AuditService,
    props: AppProperties,
) {

    private val resetTtl = props.passwordReset.ttl
    private val frontendUrl = props.frontendUrl.trimEnd('/')

    /** Always succeeds to the caller — never reveals whether the email exists. */
    @Transactional
    fun forgotPassword(email: String) {
        val user = userRepository.findByEmailIgnoreCase(email) ?: return
        val rawToken = Tokens.randomToken()
        passwordResetTokenRepository.save(
            PasswordResetToken(user.id, Tokens.sha256(rawToken), OffsetDateTime.now().plus(resetTtl)),
        )
        emailService.sendPasswordReset(user.email, "$frontendUrl/reset-password/$rawToken")
    }

    @Transactional
    fun resetPassword(rawToken: String, newPassword: String) {
        val invalid = ValidationException("Invalid or expired reset link")
        val token = passwordResetTokenRepository.findByTokenHash(Tokens.sha256(rawToken)) ?: throw invalid
        if (!token.isUsable(OffsetDateTime.now())) throw invalid
        val user = userRepository.findById(token.userId).orElseThrow { NotFoundException("User not found") }
        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
        token.markUsed(OffsetDateTime.now())
        passwordResetTokenRepository.save(token)
        tokenService.revokeAllForUser(user.id)
        auditService.record("user", user.id, "update", user.id)
    }

    @Transactional
    fun changePassword(userId: Long, currentPassword: String, newPassword: String, rawRefresh: String?) {
        val user = userRepository.findById(userId).orElseThrow { UnauthorizedException() }
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            throw ValidationException("Current password is incorrect")
        }
        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
        tokenService.revokeAllForUserExceptCurrent(user.id, rawRefresh)
        auditService.record("user", user.id, "update", user.id)
    }
}
