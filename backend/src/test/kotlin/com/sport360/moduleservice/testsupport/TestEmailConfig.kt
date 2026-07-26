package com.sport360.moduleservice.testsupport

import com.sport360.moduleservice.email.EmailService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/** Captures outbound emails so tests can read the MFA code and reset link. */
class CapturingEmailService : EmailService {

    @Volatile
    var lastMfaCode: String? = null

    @Volatile
    var lastResetLink: String? = null

    @Volatile
    var lastInviteLink: String? = null

    override fun sendMfaCode(toEmail: String, code: String) {
        lastMfaCode = code
    }

    override fun sendPasswordReset(toEmail: String, resetLink: String) {
        lastResetLink = resetLink
    }

    override fun sendInvite(toEmail: String, inviteLink: String) {
        lastInviteLink = inviteLink
    }
}

@TestConfiguration
class TestEmailConfig {

    @Bean
    @Primary
    fun capturingEmailService(): CapturingEmailService = CapturingEmailService()
}
