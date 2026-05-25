package com.sport360.moduleservice.notifications.web

import java.time.OffsetDateTime

data class NotificationResponse(
    val id: Long,
    val type: String,
    val title: String,
    val message: String?,
    val entityType: String?,
    val entityId: Long?,
    val isRead: Boolean,
    val createdAt: OffsetDateTime,
)

data class UnreadCountResponse(val count: Long)
