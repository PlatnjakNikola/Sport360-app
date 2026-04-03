package com.sport360.moduleservice.security

import com.sport360.moduleservice.config.AppProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration

/** Builds and clears the httpOnly auth cookies (env-aware Secure flag, SameSite=Strict). */
@Service
class CookieService(props: AppProperties) {

    private val secure = props.cookie.secure
    private val sameSite = props.cookie.sameSite
    private val accessTtl = props.jwt.accessTtl
    private val refreshTtl = props.jwt.refreshTtl

    fun accessCookie(token: String): ResponseCookie = build(ACCESS_COOKIE, token, accessTtl)

    fun refreshCookie(token: String): ResponseCookie = build(REFRESH_COOKIE, token, refreshTtl)

    fun clearAccessCookie(): ResponseCookie = build(ACCESS_COOKIE, "", Duration.ZERO)

    fun clearRefreshCookie(): ResponseCookie = build(REFRESH_COOKIE, "", Duration.ZERO)

    private fun build(name: String, value: String, maxAge: Duration): ResponseCookie =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(maxAge)
            .build()

    companion object {
        const val ACCESS_COOKIE = "access_token"
        const val REFRESH_COOKIE = "refresh_token"
    }
}
