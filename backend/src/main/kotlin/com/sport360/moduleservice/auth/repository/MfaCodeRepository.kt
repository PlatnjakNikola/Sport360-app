package com.sport360.moduleservice.auth.repository

import com.sport360.moduleservice.auth.domain.MfaCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface MfaCodeRepository : JpaRepository<MfaCode, Long> {

    fun findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId: Long): MfaCode?

    fun countByUserIdAndCreatedAtAfter(userId: Long, after: OffsetDateTime): Long

    @Modifying
    @Query("update MfaCode c set c.usedAt = :now where c.userId = :userId and c.usedAt is null")
    fun invalidateAllForUser(@Param("userId") userId: Long, @Param("now") now: OffsetDateTime): Int

    @Modifying
    @Query("delete from MfaCode c where c.expiresAt < :now or c.usedAt is not null")
    fun deleteExpiredOrUsed(@Param("now") now: OffsetDateTime): Int
}
