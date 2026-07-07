package com.sport360.moduleservice.servicecenters.web

import com.sport360.moduleservice.admin.service.ServiceCenterAdminService
import com.sport360.moduleservice.admin.web.CreateServiceCenterRequest
import com.sport360.moduleservice.admin.web.ServiceCenterAdminResponse
import com.sport360.moduleservice.admin.web.UpdateServiceCenterRequest
import com.sport360.moduleservice.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/service-centers")
@PreAuthorize("hasRole('ADMIN')")
class AdminServiceCenterController(private val serviceCenterAdminService: ServiceCenterAdminService) {

    @GetMapping
    fun list(): ApiResponse<List<ServiceCenterAdminResponse>> = ApiResponse.ok(serviceCenterAdminService.list())

    @PostMapping
    fun create(@Valid @RequestBody request: CreateServiceCenterRequest): ApiResponse<ServiceCenterAdminResponse> =
        ApiResponse.ok(serviceCenterAdminService.create(request))

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateServiceCenterRequest): ApiResponse<ServiceCenterAdminResponse> =
        ApiResponse.ok(serviceCenterAdminService.update(id, request))
}
