package com.sport360.moduleservice.auth.service

import com.sport360.moduleservice.auth.domain.RefreshToken
import com.sport360.moduleservice.auth.repository.RefreshTokenRepository
import com.sport360.moduleservice.common.UnauthorizedException
import com.sport360.moduleservice.config.AppProperties
import com.sport360.moduleservice.security.CookieService
import com.sport360.moduleservice.security.JwtService
import com.sport360.moduleservice.security.Tokens
import com.sport360.moduleservice.users.domain.User
import com.sport360.moduleservice.users.repository.UserRepository
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/** Refresh-token lifecycle (persist, rotate, revoke) and auth-cookie issuance. */
@Service
class TokenService(
    private val jwtService: JwtService,
    private val cookieService: CookieService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    props: AppProperties,
) {

    private val refreshTtl = props.jwt.refreshTtl

    /** Issues a fresh access + refresh pair and sets both httpOnly cookies. */
    @Transactional
    fun issueAuthCookies(user: User, response: HttpServletResponse) {
        val accessToken = jwtService.issueAccessToken(user)
        val rawRefresh = Tokens.randomToken()
        refreshTokenRepository.save(
            RefreshToken(user.id, Tokens.sha256(rawRefresh), OffsetDateTime.now().plus(refreshTtl)),
        )
        addCookie(response, cookieService.accessCookie(accessToken))
        addCookie(response, cookieService.refreshCookie(rawRefresh))
    }

    /** Rotation: validate the presented refresh token, revoke it, issue a new pair. */
    @Transactional
    fun rotate(rawRefresh: String?, response: HttpServletResponse) {
        val token = rawRefresh?.takeIf { it.isNotBlank() }
            ?.let { refreshTokenRepository.findByTokenHash(Tokens.sha256(it)) }
            ?: throw UnauthorizedException("Invalid refresh token")
        val now = OffsetDateTime.now()
        if (!token.isActive(now)) throw UnauthorizedException("Refresh token expired or revoked")
        token.revoke(now)
        val user = userRepository.findById(token.userId)
            .orElseThrow { UnauthorizedException("User not found") }
        if (!user.isActive) throw UnauthorizedException("Account is inactive")
        issueAuthCookies(user, response)
    }

    /** Revokes the presented refresh token (if any) and clears both cookies. */
    @Transactional
    fun logout(rawRefresh: String?, response: HttpServletResponse) {
        rawRefresh?.takeIf { it.isNotBlank() }
            ?.let { refreshTokenRepository.findByTokenHash(Tokens.sha256(it)) }
            ?.takeIf { it.revokedAt == null }
            ?.revoke(OffsetDateTime.now())
        addCookie(response, cookieService.clearAccessCookie())
        addCookie(response, cookieService.clearRefreshCookie())
    }

    @Transactional
    fun revokeAllForUser(userId: Long) {
        refreshTokenRepository.revokeAllForUser(userId, OffsetDateTime.now())
    }

    @Transactional
    fun revokeAllForUserExceptCurrent(userId: Long, rawRefresh: String?) {
        val now = OffsetDateTime.now()
        if (rawRefresh.isNullOrBlank()) {
            refreshTokenRepository.revokeAllForUser(userId, now)
        } else {
            refreshTokenRepository.revokeAllForUserExcept(userId, Tokens.sha256(rawRefresh), now)
        }
    }

    private fun addCookie(response: HttpServletResponse, cookie: ResponseCookie) =
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
}
