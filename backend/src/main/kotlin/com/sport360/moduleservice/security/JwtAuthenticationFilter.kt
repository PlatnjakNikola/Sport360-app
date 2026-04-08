package com.sport360.moduleservice.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Reads the access-token cookie, validates it, and populates the SecurityContext.
 * Instantiated by [SecurityConfig] (not a @Component) so Boot does not also register it
 * as a standalone servlet filter.
 */
class JwtAuthenticationFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.cookies?.firstOrNull { it.name == CookieService.ACCESS_COOKIE }?.value
        if (!token.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
            runCatching { jwtService.parseAccessToken(token) }.getOrNull()?.let { claims ->
                val authority = SimpleGrantedAuthority("ROLE_${claims.role.uppercase()}")
                val authentication = UsernamePasswordAuthenticationToken(
                    AuthPrincipal(claims.userId, claims.role),
                    null,
                    listOf(authority),
                )
                SecurityContextHolder.getContext().authentication = authentication
            }
        }
        filterChain.doFilter(request, response)
    }
}
