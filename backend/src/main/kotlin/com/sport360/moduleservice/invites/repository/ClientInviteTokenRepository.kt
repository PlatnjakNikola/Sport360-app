package com.sport360.moduleservice.invites.repository

import com.sport360.moduleservice.invites.domain.ClientInviteToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface ClientInviteTokenRepository : JpaRepository<ClientInviteToken, Long> {

    fun findByTokenHash(tokenHash: String): ClientInviteToken?

    fun findAllByUsedAtIsNullOrderByCreatedAtDesc(): List<ClientInviteToken>

    fun existsByEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(email: String, now: OffsetDateTime): Boolean

    @Modifying
    @Query("delete from ClientInviteToken t where t.expiresAt < :now or t.usedAt is not null")
    fun deleteExpiredOrUsed(@Param("now") now: OffsetDateTime): Int
}
