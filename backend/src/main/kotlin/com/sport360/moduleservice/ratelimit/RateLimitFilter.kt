package com.sport360.moduleservice.ratelimit

import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.common.ApiError
import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.ErrorCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

/** Per-IP rate limiting on sensitive auth endpoints. Runs before the security chain. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RateLimitFilter(
    private val rateLimiter: RateLimiter,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val policy = policyFor(request)
        if (policy != null) {
            val key = "${policy.name}:${clientIp(request)}"
            if (!rateLimiter.tryConsume(key, policy.capacity, policy.window)) {
                writeTooManyRequests(response)
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun policyFor(request: HttpServletRequest): Policy? {
        val uri = request.requestURI
        if (request.method.equals("GET", ignoreCase = true)) {
            if (uri.startsWith("/api/v1/public/modules/") && uri.endsWith("/history")) {
                return Policy("public-history", 20, Duration.ofMinutes(1))
            }
            return null
        }
        if (!request.method.equals("POST", ignoreCase = true)) return null
        return when (uri) {
            "/api/v1/auth/login" -> Policy("login", 5, Duration.ofMinutes(1))
            "/api/v1/auth/mfa/verify" -> Policy("mfa-verify", 5, Duration.ofMinutes(1))
            "/api/v1/auth/mfa/resend" -> Policy("mfa-resend", 3, Duration.ofMinutes(1))
            "/api/v1/auth/forgot-password" -> Policy("forgot-password", 3, Duration.ofHours(1))
            else -> null
        }
    }

    private fun clientIp(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            ?: request.remoteAddr

    private fun writeTooManyRequests(response: HttpServletResponse) {
        response.status = ErrorCode.RATE_LIMITED.status.value()
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                ApiResponse.failure(ApiError(ErrorCode.RATE_LIMITED.name, "Too many requests, please try again later")),
            ),
        )
    }

    private data class Policy(val name: String, val capacity: Long, val window: Duration)
}
