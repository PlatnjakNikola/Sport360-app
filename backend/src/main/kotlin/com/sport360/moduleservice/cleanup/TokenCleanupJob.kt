package com.sport360.moduleservice.cleanup

import com.sport360.moduleservice.auth.repository.MfaCodeRepository
import com.sport360.moduleservice.auth.repository.PasswordResetTokenRepository
import com.sport360.moduleservice.auth.repository.RefreshTokenRepository
import com.sport360.moduleservice.invites.repository.ClientInviteTokenRepository
import com.sport360.moduleservice.invites.repository.TechnicianInviteTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/** Daily purge of expired/consumed auth and invite tokens so the tables stay small. */
@Component
class TokenCleanupJob(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val mfaCodeRepository: MfaCodeRepository,
    private val technicianInviteTokenRepository: TechnicianInviteTokenRepository,
    private val clientInviteTokenRepository: ClientInviteTokenRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.cleanup.cron:0 30 3 * * *}")
    @Transactional
    fun purgeExpiredTokens() {
        val now = OffsetDateTime.now()
        val refresh = refreshTokenRepository.deleteExpiredOrRevoked(now)
        val reset = passwordResetTokenRepository.deleteExpiredOrUsed(now)
        val mfa = mfaCodeRepository.deleteExpiredOrUsed(now)
        val techInvites = technicianInviteTokenRepository.deleteExpiredOrUsed(now)
        val clientInvites = clientInviteTokenRepository.deleteExpiredOrUsed(now)
        log.info(
            "Token cleanup removed {} refresh, {} password-reset, {} MFA, {} technician-invite, {} client-invite rows",
            refresh, reset, mfa, techInvites, clientInvites,
        )
    }
}
