package com.sport360.moduleservice.technicianworkflow.web

import com.sport360.moduleservice.packages.web.ActivityItem
import com.sport360.moduleservice.packages.web.StatusCount
import com.sport360.moduleservice.packages.web.TimelineEntry
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime

data class NextStatusRequest(
    val returnTrackingLink: String? = null,
)

data class CreateInternalPackageRequest(
    @field:NotBlank @field:Size(max = 100) val packageNumber: String,
    val description: String? = null,
    val note: String? = null,
    @field:Min(0) val approxQuantity: Int? = null,
)

data class TechnicianPackageSummaryResponse(
    val id: Long,
    val packageNumber: String,
    val statusCode: String,
    val statusName: String,
    val isInternal: Boolean,
    val createdAt: OffsetDateTime,
    val receivedAt: OffsetDateTime?,
    val totalModules: Long,
    val finishedModules: Long,
    val totalValue: BigDecimal,
)

data class TechnicianPackageDetailResponse(
    val id: Long,
    val packageNumber: String,
    val statusCode: String,
    val statusName: String,
    val isInternal: Boolean,
    val outboundTrackingLink: String?,
    val returnTrackingLink: String?,
    val description: String?,
    val note: String?,
    val approxQuantity: Int?,
    val createdAt: OffsetDateTime,
    val receivedAt: OffsetDateTime?,
    val serviceStartedAt: OffsetDateTime?,
    val serviceCompletedAt: OffsetDateTime?,
    val shippedAt: OffsetDateTime?,
    val arrivedAt: OffsetDateTime?,
    val totalModules: Long,
    val finishedModules: Long,
    val timeline: List<TimelineEntry>,
)

data class PersonalStats(
    val modulesToday: Long,
    val modulesThisWeek: Long,
    val repairRatePercent: Int,
    val totalValueThisWeek: BigDecimal,
)

data class TechnicianDashboardResponse(
    val statusCounts: List<StatusCount>,
    val personalStats: PersonalStats,
    val recentActivity: List<ActivityItem>,
)
