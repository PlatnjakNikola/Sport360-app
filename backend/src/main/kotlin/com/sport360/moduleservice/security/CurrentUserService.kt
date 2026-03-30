package com.sport360.moduleservice.security

import com.sport360.moduleservice.common.UnauthorizedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

/** Resolves the currently authenticated user from the SecurityContext. */
@Service
class CurrentUserService {

    fun currentUserId(): Long = currentPrincipal().userId

    fun currentPrincipal(): AuthPrincipal {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return principal as? AuthPrincipal ?: throw UnauthorizedException()
    }
}
