package com.sport360.moduleservice.auth.repository

import com.sport360.moduleservice.auth.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Modifying
    @Query("update RefreshToken r set r.revokedAt = :now where r.userId = :userId and r.revokedAt is null")
    fun revokeAllForUser(@Param("userId") userId: Long, @Param("now") now: OffsetDateTime): Int

    @Modifying
    @Query(
        "update RefreshToken r set r.revokedAt = :now " +
            "where r.userId = :userId and r.revokedAt is null and r.tokenHash <> :exceptHash"
    )
    fun revokeAllForUserExcept(
        @Param("userId") userId: Long,
        @Param("exceptHash") exceptHash: String,
        @Param("now") now: OffsetDateTime,
    ): Int

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :now or r.revokedAt is not null")
    fun deleteExpiredOrRevoked(@Param("now") now: OffsetDateTime): Int
}
