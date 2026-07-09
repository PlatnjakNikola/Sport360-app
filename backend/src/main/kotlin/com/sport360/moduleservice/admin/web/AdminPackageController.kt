package com.sport360.moduleservice.admin.web

import com.sport360.moduleservice.admin.service.AdminPackageService
import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.Pageables
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/admin/packages")
@PreAuthorize("hasRole('ADMIN')")
class AdminPackageController(private val adminPackageService: AdminPackageService) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "false") includeDeleted: Boolean,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) clientId: Long?,
        @RequestParam(required = false) serviceCenterId: Long?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateTo: OffsetDateTime?,
    ): ApiResponse<PageResponse<AdminPackageSummaryResponse>> =
        ApiResponse.ok(
            adminPackageService.list(
                includeDeleted, status, clientId, serviceCenterId, type, search, dateFrom, dateTo, Pageables.of(page, limit),
            ),
        )

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<AdminPackageDetailResponse> =
        ApiResponse.ok(adminPackageService.detail(id))

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: AdminUpdatePackageRequest): ApiResponse<AdminPackageDetailResponse> =
        ApiResponse.ok(adminPackageService.update(id, request))

    @PatchMapping("/{id}/status")
    fun overrideStatus(@PathVariable id: Long, @Valid @RequestBody request: OverrideStatusRequest): ApiResponse<AdminPackageDetailResponse> =
        ApiResponse.ok(adminPackageService.overrideStatus(id, request.statusId))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ApiResponse<Unit> {
        adminPackageService.softDelete(id)
        return ApiResponse.ok(Unit)
    }

    @PostMapping("/{id}/restore")
    fun restore(@PathVariable id: Long): ApiResponse<AdminPackageDetailResponse> =
        ApiResponse.ok(adminPackageService.restore(id))

    @PostMapping("/bulk-delete")
    fun bulkDelete(@Valid @RequestBody request: BulkDeleteRequest): ApiResponse<BulkResultResponse> =
        ApiResponse.ok(BulkResultResponse(adminPackageService.bulkDelete(request.packageIds)))

    @PostMapping("/bulk-status")
    fun bulkStatus(@Valid @RequestBody request: BulkStatusRequest): ApiResponse<BulkResultResponse> =
        ApiResponse.ok(BulkResultResponse(adminPackageService.bulkStatus(request.packageIds, request.statusId)))

    @PostMapping("/{id}/modules")
    fun addCorrectionModule(@PathVariable id: Long, @Valid @RequestBody request: AdminCreateModuleRequest): ApiResponse<AdminModuleDetailResponse> =
        ApiResponse.ok(adminPackageService.addCorrectionModule(id, request))
}
