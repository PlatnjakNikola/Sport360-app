package com.sport360.moduleservice.admin.web

import com.sport360.moduleservice.modules.web.ModuleImageResponse
import com.sport360.moduleservice.packages.web.TimelineEntry
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime

// ---- Packages ----

data class AdminPackageSummaryResponse(
    val id: Long,
    val packageNumber: String,
    val companyName: String?,
    val isInternal: Boolean,
    val statusCode: String,
    val statusName: String,
    val serviceCenterId: Long?,
    val createdAt: OffsetDateTime,
    val receivedAt: OffsetDateTime?,
    val shippedAt: OffsetDateTime?,
    val deleted: Boolean,
    val totalModules: Long,
    val repairedCount: Long,
    val notRepairableCount: Long,
    val totalValue: BigDecimal,
)

data class AdminModuleSummaryResponse(
    val id: Long,
    val moduleNumber: String,
    val problemTypeName: String,
    val statusCode: String,
    val statusName: String,
    val technicianName: String,
    val price: BigDecimal?,
    val thumbnailUrl: String?,
)

data class AdminPackageDetailResponse(
    val id: Long,
    val packageNumber: String,
    val clientId: Long?,
    val companyName: String?,
    val isInternal: Boolean,
    val serviceCenterId: Long?,
    val statusCode: String,
    val statusName: String,
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
    val deletedAt: OffsetDateTime?,
    val totalModules: Long,
    val repairedCount: Long,
    val notRepairableCount: Long,
    val totalValue: BigDecimal,
    val averagePrice: BigDecimal,
    val timeline: List<TimelineEntry>,
    val modules: List<AdminModuleSummaryResponse>,
)

data class AdminUpdatePackageRequest(
    @field:Size(max = 100) val packageNumber: String? = null,
    val description: String? = null,
    val note: String? = null,
    @field:Size(max = 2048) val outboundTrackingLink: String? = null,
    @field:Size(max = 2048) val returnTrackingLink: String? = null,
)

data class OverrideStatusRequest(
    @field:NotNull val statusId: Short,
)

data class BulkDeleteRequest(
    @field:NotEmpty val packageIds: List<Long>,
)

data class BulkStatusRequest(
    @field:NotEmpty val packageIds: List<Long>,
    @field:NotNull val statusId: Short,
)

data class BulkResultResponse(val affected: Int)

data class AdminCreateModuleRequest(
    @field:NotBlank @field:Size(max = 150) val moduleNumber: String,
    @field:NotNull val problemTypeId: Short,
    @field:NotNull val assignedTechnicianId: Long,
)

// ---- Modules ----

data class AdminModuleDetailResponse(
    val id: Long,
    val packageId: Long,
    val packageNumber: String,
    val moduleNumber: String,
    val problemTypeId: Short,
    val problemTypeName: String,
    val statusCode: String,
    val statusName: String,
    val technicianId: Long,
    val technicianName: String,
    val pixelsRepaired: Int?,
    val chipsReplaced: Int?,
    val repairNote: String?,
    val price: BigDecimal?,
    val decisionStatusCode: String?,
    val createdAt: OffsetDateTime,
    val completedAt: OffsetDateTime?,
    val deletedAt: OffsetDateTime?,
    val images: List<ModuleImageResponse>,
)

data class AdminUpdateModuleRequest(
    val problemTypeId: Short? = null,
    val assignedTechnicianId: Long? = null,
    val statusCode: String? = null,
    @field:Min(0) val pixelsRepaired: Int? = null,
    @field:Min(0) val chipsReplaced: Int? = null,
    @field:DecimalMin("0.0") val price: BigDecimal? = null,
    val repairNote: String? = null,
)

// ---- Trash ----

data class TrashItemResponse(
    val entityType: String,
    val id: Long,
    val label: String,
    val packageId: Long?,
    val deletedAt: OffsetDateTime?,
)

// ---- Lookups ----

data class StatusOptionResponse(val id: Short, val code: String, val name: String, val sortOrder: Short)

// ---- Catalogs: problem types ----

data class ProblemTypeAdminResponse(
    val id: Short,
    val code: String,
    val name: String,
    val sortOrder: Short,
    val active: Boolean,
    val usageCount: Long,
)

data class CreateProblemTypeRequest(
    @field:NotBlank @field:Size(max = 50) val code: String,
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotNull val sortOrder: Short,
)

data class UpdateProblemTypeRequest(
    @field:Size(max = 255) val name: String? = null,
    val sortOrder: Short? = null,
    val active: Boolean? = null,
)

// ---- Catalogs: service centers ----

data class ServiceCenterAdminResponse(
    val id: Long,
    val code: String,
    val name: String,
    val country: String?,
    val city: String?,
    val address: String?,
    val active: Boolean,
    val technicianCount: Long,
)

data class CreateServiceCenterRequest(
    @field:NotBlank @field:Size(max = 50) val code: String,
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:Size(max = 100) val country: String? = null,
    @field:Size(max = 100) val city: String? = null,
    val address: String? = null,
)

data class UpdateServiceCenterRequest(
    @field:Size(max = 255) val name: String? = null,
    @field:Size(max = 100) val country: String? = null,
    @field:Size(max = 100) val city: String? = null,
    val address: String? = null,
    val active: Boolean? = null,
)
