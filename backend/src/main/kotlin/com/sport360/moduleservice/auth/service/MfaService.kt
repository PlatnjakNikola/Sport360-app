package com.sport360.moduleservice.auth.service

import com.sport360.moduleservice.auth.domain.MfaCode
import com.sport360.moduleservice.auth.repository.MfaCodeRepository
import com.sport360.moduleservice.common.RateLimitedException
import com.sport360.moduleservice.common.UnauthorizedException
import com.sport360.moduleservice.config.AppProperties
import com.sport360.moduleservice.email.EmailService
import com.sport360.moduleservice.users.domain.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.OffsetDateTime

/** Admin 6-digit email MFA: code creation/sending, verification with attempt limiting, resend limiting. */
@Service
class MfaService(
    private val mfaCodeRepository: MfaCodeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
    props: AppProperties,
) {

    private val codeTtl = props.mfa.codeTtl
    private val maxAttempts = props.mfa.maxAttempts
    private val maxResends = props.mfa.maxResends
    private val random = SecureRandom()

    @Transactional
    fun createAndSend(user: User) {
        val now = OffsetDateTime.now()
        mfaCodeRepository.invalidateAllForUser(user.id, now)
        val code = generateCode()
        mfaCodeRepository.save(MfaCode(user.id, passwordEncoder.encode(code), now.plus(codeTtl)))
        emailService.sendMfaCode(user.email, code)
    }

    @Transactional
    fun verify(userId: Long, code: String) {
        val now = OffsetDateTime.now()
        val mfa = mfaCodeRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId)
            ?: throw UnauthorizedException("No active verification code")
        if (mfa.expiresAt.isBefore(now)) throw UnauthorizedException("Verification code expired")
        if (mfa.attempts >= maxAttempts) throw UnauthorizedException("Too many attempts; request a new code")
        if (!passwordEncoder.matches(code, mfa.codeHash)) {
            mfa.incrementAttempts()
            mfaCodeRepository.save(mfa)
            throw UnauthorizedException("Invalid verification code")
        }
        mfa.markUsed(now)
        mfaCodeRepository.save(mfa)
    }

    @Transactional
    fun resend(user: User) {
        val recent = mfaCodeRepository.countByUserIdAndCreatedAtAfter(user.id, OffsetDateTime.now().minus(codeTtl))
        if (recent > maxResends) throw RateLimitedException("Resend limit reached; try again later")
        createAndSend(user)
    }

    private fun generateCode(): String = "%06d".format(random.nextInt(1_000_000))
}
