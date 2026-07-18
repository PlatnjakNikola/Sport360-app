package com.sport360.moduleservice.audit.web

import java.time.OffsetDateTime

data class AuditLogResponse(
    val id: Long,
    val entityType: String,
    val entityId: Long,
    val actionType: String,
    val changedByUserId: Long,
    val changedByName: String?,
    val oldValueJson: String?,
    val newValueJson: String?,
    val createdAt: OffsetDateTime,
)
