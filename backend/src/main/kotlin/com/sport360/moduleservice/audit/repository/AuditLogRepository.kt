package com.sport360.moduleservice.audit.repository

import com.sport360.moduleservice.audit.domain.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface AuditLogProjection {
    val id: Long
    val entityType: String
    val entityId: Long
    val actionType: String
    val changedByUserId: Long
    val changedByName: String?
    val oldValueJson: String?
    val newValueJson: String?
    val createdAt: OffsetDateTime
}

interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    @Query(
        value = """
            select a.id as id, a.entityType as entityType, a.entityId as entityId, a.actionType as actionType,
                   a.changedByUserId as changedByUserId, u.name as changedByName,
                   a.oldValueJson as oldValueJson, a.newValueJson as newValueJson, a.createdAt as createdAt
            from AuditLog a
            left join com.sport360.moduleservice.users.domain.User u on u.id = a.changedByUserId
            where (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:userId is null or a.changedByUserId = :userId)
              and (:actionType is null or a.actionType = :actionType)
              and a.createdAt >= :from and a.createdAt <= :to
            order by a.createdAt desc
        """,
        countQuery = """
            select count(a) from AuditLog a
            where (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:userId is null or a.changedByUserId = :userId)
              and (:actionType is null or a.actionType = :actionType)
              and a.createdAt >= :from and a.createdAt <= :to
        """,
    )
    fun search(
        @Param("entityType") entityType: String?,
        @Param("entityId") entityId: Long?,
        @Param("userId") userId: Long?,
        @Param("actionType") actionType: String?,
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
        pageable: Pageable,
    ): Page<AuditLogProjection>
}
