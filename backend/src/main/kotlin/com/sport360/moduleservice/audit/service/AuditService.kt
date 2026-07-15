package com.sport360.moduleservice.audit.service

import com.sport360.moduleservice.audit.domain.AuditLog
import com.sport360.moduleservice.audit.repository.AuditLogRepository
import com.sport360.moduleservice.audit.web.AuditLogResponse
import com.sport360.moduleservice.common.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Writes audit_log rows and serves the filtered admin audit-log list. Never store secrets in snapshots. */
@Service
class AuditService(private val auditLogRepository: AuditLogRepository) {

    fun record(
        entityType: String,
        entityId: Long,
        actionType: String,
        changedByUserId: Long,
        oldValueJson: String? = null,
        newValueJson: String? = null,
    ) {
        auditLogRepository.save(
            AuditLog(entityType, entityId, actionType, changedByUserId, oldValueJson, newValueJson),
        )
    }

    @Transactional(readOnly = true)
    fun list(
        entityType: String?,
        entityId: Long?,
        userId: Long?,
        actionType: String?,
        from: OffsetDateTime?,
        to: OffsetDateTime?,
        pageable: Pageable,
    ): PageResponse<AuditLogResponse> {
        val page = auditLogRepository.search(
            entityType?.takeIf { it.isNotBlank() },
            entityId,
            userId,
            actionType?.takeIf { it.isNotBlank() },
            from ?: DATE_FLOOR,
            to ?: DATE_CEILING,
            pageable,
        )
        return PageResponse.from(page) {
            AuditLogResponse(
                id = it.id,
                entityType = it.entityType,
                entityId = it.entityId,
                actionType = it.actionType,
                changedByUserId = it.changedByUserId,
                changedByName = it.changedByName,
                oldValueJson = it.oldValueJson,
                newValueJson = it.newValueJson,
                createdAt = it.createdAt,
            )
        }
    }

    private companion object {
        val DATE_FLOOR: OffsetDateTime = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val DATE_CEILING: OffsetDateTime = OffsetDateTime.of(2200, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    }
}
