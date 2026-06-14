package com.sport360.moduleservice.packages.web

import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.Pageables
import com.sport360.moduleservice.modules.service.ModuleService
import com.sport360.moduleservice.modules.web.ClientModuleResponse
import com.sport360.moduleservice.packages.service.ClientPackageService
import com.sport360.moduleservice.security.CurrentUserService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/client")
@PreAuthorize("hasRole('CLIENT')")
class ClientPackageController(
    private val clientPackageService: ClientPackageService,
    private val moduleService: ModuleService,
    private val currentUserService: CurrentUserService,
) {

    @GetMapping("/dashboard")
    fun dashboard(): ApiResponse<ClientDashboardResponse> =
        ApiResponse.ok(clientPackageService.dashboard(currentUserService.currentUserId()))

    @GetMapping("/packages")
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) search: String?,
    ): ApiResponse<PageResponse<PackageSummaryResponse>> =
        ApiResponse.ok(
            clientPackageService.listPackages(currentUserService.currentUserId(), status, search, Pageables.of(page, limit)),
        )

    @PostMapping("/packages")
    fun create(@Valid @RequestBody request: CreatePackageRequest): ApiResponse<PackageDetailResponse> =
        ApiResponse.ok(clientPackageService.createPackage(currentUserService.currentUserId(), request))

    @GetMapping("/packages/{id}")
    fun get(@PathVariable id: Long): ApiResponse<PackageDetailResponse> =
        ApiResponse.ok(clientPackageService.getPackage(currentUserService.currentUserId(), id))

    @PatchMapping("/packages/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePackageRequest,
    ): ApiResponse<PackageDetailResponse> =
        ApiResponse.ok(clientPackageService.updatePackage(currentUserService.currentUserId(), id, request))

    @PostMapping("/packages/{id}/mark-sent")
    fun markSent(@PathVariable id: Long): ApiResponse<PackageDetailResponse> =
        ApiResponse.ok(clientPackageService.markSent(currentUserService.currentUserId(), id))

    @PostMapping("/packages/{id}/confirm-arrival")
    fun confirmArrival(@PathVariable id: Long): ApiResponse<PackageDetailResponse> =
        ApiResponse.ok(clientPackageService.confirmArrival(currentUserService.currentUserId(), id))

    @GetMapping("/packages/{id}/modules")
    fun modules(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ApiResponse<PageResponse<ClientModuleResponse>> =
        ApiResponse.ok(moduleService.listClientModules(currentUserService.currentUserId(), id, Pageables.of(page, limit)))

    @GetMapping("/modules/{id}")
    fun moduleDetail(@PathVariable id: Long): ApiResponse<ClientModuleResponse> =
        ApiResponse.ok(moduleService.clientModuleDetail(currentUserService.currentUserId(), id))
}
