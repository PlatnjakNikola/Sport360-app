package com.sport360.moduleservice.email

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

/** Prod email impl: sends via the configured SMTP provider (JavaMailSender). */
@Service
@Profile("prod")
class SmtpEmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail.from:no-reply@module-service.local}") private val from: String,
) : EmailService {

    override fun sendMfaCode(toEmail: String, code: String) =
        send(toEmail, "Your verification code", "Your verification code is $code. It is valid for 10 minutes.")

    override fun sendPasswordReset(toEmail: String, resetLink: String) =
        send(toEmail, "Reset your password", "Use this link to reset your password (valid 1 hour):\n$resetLink")

    override fun sendInvite(toEmail: String, inviteLink: String) =
        send(toEmail, "You're invited to Module Service", "Set up your account here (valid 48 hours):\n$inviteLink")

    private fun send(to: String, subject: String, body: String) {
        val message = SimpleMailMessage().apply {
            setFrom(from)
            setTo(to)
            setSubject(subject)
            setText(body)
        }
        mailSender.send(message)
    }
}
