package com.sport360.moduleservice.security

import com.sport360.moduleservice.common.UnauthorizedException
import com.sport360.moduleservice.config.AppProperties
import com.sport360.moduleservice.users.domain.User
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

/** Claims carried by an access token. */
data class AccessClaims(val userId: Long, val role: String)

/** Issues and parses the stateless access token and the short-lived MFA token (jjwt, HS256). */
@Service
class JwtService(props: AppProperties) {

    private val key = Keys.hmacShaKeyFor(props.jwt.secret.toByteArray(StandardCharsets.UTF_8))
    private val accessTtl = props.jwt.accessTtl
    private val mfaTtl = props.jwt.mfaTtl

    fun issueAccessToken(user: User): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("role", user.role.code)
            .claim("typ", TYPE_ACCESS)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(accessTtl)))
            .signWith(key)
            .compact()
    }

    fun issueMfaToken(userId: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("typ", TYPE_MFA)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(mfaTtl)))
            .signWith(key)
            .compact()
    }

    fun parseAccessToken(token: String): AccessClaims {
        val claims = parse(token)
        if (claims["typ"] != TYPE_ACCESS) throw UnauthorizedException("Invalid token type")
        val userId = claims.subject.toLong()
        val role = claims["role"] as? String ?: throw UnauthorizedException("Missing role claim")
        return AccessClaims(userId, role)
    }

    fun parseMfaToken(token: String): Long {
        val claims = parse(token)
        if (claims["typ"] != TYPE_MFA) throw UnauthorizedException("Invalid login session")
        return claims.subject.toLong()
    }

    private fun parse(token: String) =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        } catch (ex: JwtException) {
            throw UnauthorizedException("Invalid or expired token")
        }

    private companion object {
        const val TYPE_ACCESS = "access"
        const val TYPE_MFA = "mfa"
    }
}
