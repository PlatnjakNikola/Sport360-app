package com.sport360.moduleservice.email

/** Outbound email abstraction so the provider can be swapped without touching callers. */
interface EmailService {
    fun sendMfaCode(toEmail: String, code: String)
    fun sendPasswordReset(toEmail: String, resetLink: String)
    fun sendInvite(toEmail: String, inviteLink: String)
}
