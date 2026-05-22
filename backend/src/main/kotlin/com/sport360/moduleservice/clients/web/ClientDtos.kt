package com.sport360.moduleservice.clients.web

import java.time.OffsetDateTime

data class ClientResponse(
    val userId: Long,
    val companyName: String,
    val contactName: String,
    val email: String,
    val contactPhone: String?,
    val address: String?,
    val active: Boolean,
    val createdAt: OffsetDateTime,
)
