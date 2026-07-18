package com.sport360.moduleservice.audit.web

import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.Pageables
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
class AdminAuditController(private val auditService: AuditService) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) entityType: String?,
        @RequestParam(required = false) entityId: Long?,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) actionType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateTo: OffsetDateTime?,
    ): ApiResponse<PageResponse<AuditLogResponse>> =
        ApiResponse.ok(auditService.list(entityType, entityId, userId, actionType, dateFrom, dateTo, Pageables.of(page, limit)))
}
