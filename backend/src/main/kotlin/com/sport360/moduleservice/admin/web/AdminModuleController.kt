package com.sport360.moduleservice.admin.web

import com.sport360.moduleservice.admin.service.AdminModuleService
import com.sport360.moduleservice.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/modules")
@PreAuthorize("hasRole('ADMIN')")
class AdminModuleController(private val adminModuleService: AdminModuleService) {

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<AdminModuleDetailResponse> =
        ApiResponse.ok(adminModuleService.detail(id))

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: AdminUpdateModuleRequest): ApiResponse<AdminModuleDetailResponse> =
        ApiResponse.ok(adminModuleService.update(id, request))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ApiResponse<Unit> {
        adminModuleService.softDelete(id)
        return ApiResponse.ok(Unit)
    }

    @PostMapping("/{id}/restore")
    fun restore(@PathVariable id: Long): ApiResponse<AdminModuleDetailResponse> =
        ApiResponse.ok(adminModuleService.restore(id))
}
