package com.sport360.moduleservice.admin.web

import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.modules.repository.ModuleStatusRepository
import com.sport360.moduleservice.packages.repository.PackageStatusRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Lookup lists for admin forms (status dropdowns for overrides and module edits). */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminLookupController(
    private val packageStatusRepository: PackageStatusRepository,
    private val moduleStatusRepository: ModuleStatusRepository,
) {

    @GetMapping("/package-statuses")
    fun packageStatuses(): ApiResponse<List<StatusOptionResponse>> =
        ApiResponse.ok(
            packageStatusRepository.findAll().sortedBy { it.sortOrder }
                .map { StatusOptionResponse(it.id, it.code, it.name, it.sortOrder) },
        )

    @GetMapping("/module-statuses")
    fun moduleStatuses(): ApiResponse<List<StatusOptionResponse>> =
        ApiResponse.ok(
            moduleStatusRepository.findAll().sortedBy { it.sortOrder }
                .map { StatusOptionResponse(it.id, it.code, it.name, it.sortOrder) },
        )
}
