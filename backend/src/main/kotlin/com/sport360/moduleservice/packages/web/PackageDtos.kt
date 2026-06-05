package com.sport360.moduleservice.packages.web

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class CreatePackageRequest(
    @field:NotBlank @field:Size(max = 100) val packageNumber: String,
    @field:Size(max = 2048) val outboundTrackingLink: String? = null,
    @field:NotBlank val description: String,
    val note: String? = null,
    @field:Min(0) val approxQuantity: Int? = null,
)

data class UpdatePackageRequest(
    @field:Size(max = 2048) val outboundTrackingLink: String? = null,
    val note: String? = null,
    val description: String? = null,
)

data class PackageSummaryResponse(
    val id: Long,
    val packageNumber: String,
    val statusCode: String,
    val statusName: String,
    val outboundTrackingLink: String?,
    val returnTrackingLink: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class TimelineEntry(
    val statusCode: String,
    val statusName: String,
    val changedAt: OffsetDateTime,
)

data class PackageDetailResponse(
    val id: Long,
    val packageNumber: String,
    val statusCode: String,
    val statusName: String,
    val outboundTrackingLink: String?,
    val returnTrackingLink: String?,
    val description: String?,
    val note: String?,
    val approxQuantity: Int?,
    val createdAt: OffsetDateTime,
    val receivedAt: OffsetDateTime?,
    val serviceCompletedAt: OffsetDateTime?,
    val shippedAt: OffsetDateTime?,
    val arrivedAt: OffsetDateTime?,
    val timeline: List<TimelineEntry>,
)

data class StatusCount(val code: String, val name: String, val count: Long)

data class ActivityItem(
    val packageId: Long,
    val packageNumber: String,
    val statusCode: String,
    val statusName: String,
    val changedAt: OffsetDateTime,
)

data class ClientDashboardResponse(
    val statusCounts: List<StatusCount>,
    val recentActivity: List<ActivityItem>,
)
