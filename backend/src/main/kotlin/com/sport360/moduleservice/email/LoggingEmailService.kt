package com.sport360.moduleservice.email

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/** Default email impl: writes the message to the console instead of sending it. */
@Service
@ConditionalOnProperty(name = ["app.mail.mode"], havingValue = "log", matchIfMissing = true)
class LoggingEmailService : EmailService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendMfaCode(toEmail: String, code: String) {
        log.info("[DEV EMAIL] MFA code for {} -> {}", toEmail, code)
    }

    override fun sendPasswordReset(toEmail: String, resetLink: String) {
        log.info("[DEV EMAIL] Password reset for {} -> {}", toEmail, resetLink)
    }

    override fun sendInvite(toEmail: String, inviteLink: String) {
        log.info("[DEV EMAIL] Invite for {} -> {}", toEmail, inviteLink)
    }
}
