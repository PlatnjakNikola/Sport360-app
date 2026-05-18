package com.sport360.moduleservice.technicians.web

import java.time.OffsetDateTime

data class TechnicianResponse(
    val userId: Long,
    val name: String,
    val email: String,
    val phone: String?,
    val serviceCenterId: Long,
    val serviceCenterName: String,
    val active: Boolean,
    val createdAt: OffsetDateTime,
)

data class UpdateTechnicianRequest(
    val name: String? = null,
    val phone: String? = null,
    val serviceCenterId: Long? = null,
    val isActive: Boolean? = null,
)
