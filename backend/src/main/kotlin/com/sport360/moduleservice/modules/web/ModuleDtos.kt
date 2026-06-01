package com.sport360.moduleservice.modules.web

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CreateModuleRequest(
    @field:NotBlank @field:Size(max = 150) val moduleNumber: String,
    @field:NotNull val problemTypeId: Short?,
)

data class RepairRequest(
    @field:NotBlank
    @field:Pattern(regexp = "repaired|not_repairable", message = "decision must be 'repaired' or 'not_repairable'")
    val decision: String,
    @field:Min(0) val pixelsRepaired: Int? = null,
    @field:Min(0) val chipsReplaced: Int? = null,
    val repairNote: String? = null,
    @field:DecimalMin("0.0") val price: BigDecimal? = null,
)

/** A stored module image: just the id and the URL that serves its bytes. */
data class ModuleImageResponse(
    val id: Long,
    val url: String,
)

/** Technician-facing module summary (includes technician name). */
data class ModuleSummaryResponse(
    val id: Long,
    val moduleNumber: String,
    val problemTypeName: String,
    val statusCode: String,
    val statusName: String,
    val technicianName: String,
    val price: BigDecimal?,
    val thumbnailUrl: String?,
)

/** Technician-facing module detail. */
data class ModuleDetailResponse(
    val id: Long,
    val packageId: Long,
    val moduleNumber: String,
    val problemTypeName: String,
    val statusCode: String,
    val statusName: String,
    val technicianName: String,
    val pixelsRepaired: Int?,
    val chipsReplaced: Int?,
    val repairNote: String?,
    val price: BigDecimal?,
    val decisionStatusCode: String?,
    val completedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val images: List<ModuleImageResponse>,
)

/** Client-facing module view — never includes the technician name (privacy). */
data class ClientModuleResponse(
    val id: Long,
    val packageId: Long,
    val moduleNumber: String,
    val problemTypeName: String,
    val statusCode: String,
    val statusName: String,
    val pixelsRepaired: Int?,
    val chipsReplaced: Int?,
    val repairNote: String?,
    val price: BigDecimal?,
    val images: List<ModuleImageResponse>,
)
