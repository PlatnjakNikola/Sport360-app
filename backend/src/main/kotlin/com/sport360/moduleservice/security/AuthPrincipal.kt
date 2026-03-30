package com.sport360.moduleservice.security

/** Authenticated principal carried in the SecurityContext, derived from the access token. */
data class AuthPrincipal(
    val userId: Long,
    val role: String,
)
