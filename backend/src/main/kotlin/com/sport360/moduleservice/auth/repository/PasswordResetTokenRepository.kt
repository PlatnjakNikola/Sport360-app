package com.sport360.moduleservice.auth.repository

import com.sport360.moduleservice.auth.domain.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {

    fun findByTokenHash(tokenHash: String): PasswordResetToken?

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now or t.usedAt is not null")
    fun deleteExpiredOrUsed(@Param("now") now: OffsetDateTime): Int
}
