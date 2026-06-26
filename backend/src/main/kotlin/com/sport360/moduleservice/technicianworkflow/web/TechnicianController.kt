package com.sport360.moduleservice.technicianworkflow.web

import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.Pageables
import com.sport360.moduleservice.images.service.ImageService
import com.sport360.moduleservice.modules.service.ModuleService
import com.sport360.moduleservice.modules.web.CreateModuleRequest
import com.sport360.moduleservice.modules.web.ModuleDetailResponse
import com.sport360.moduleservice.modules.web.ModuleImageResponse
import com.sport360.moduleservice.modules.web.ModuleSummaryResponse
import com.sport360.moduleservice.modules.web.RepairRequest
import com.sport360.moduleservice.problemtypes.service.ProblemTypeService
import com.sport360.moduleservice.problemtypes.web.ProblemTypeResponse
import com.sport360.moduleservice.technicianworkflow.service.TechnicianWorkflowService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/technician")
@PreAuthorize("hasRole('TECHNICIAN')")
class TechnicianController(
    private val technicianWorkflowService: TechnicianWorkflowService,
    private val moduleService: ModuleService,
    private val problemTypeService: ProblemTypeService,
    private val imageService: ImageService,
) {

    @GetMapping("/dashboard")
    fun dashboard(): ApiResponse<TechnicianDashboardResponse> = ApiResponse.ok(technicianWorkflowService.dashboard())

    @GetMapping("/packages/active")
    fun active(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) search: String?,
    ): ApiResponse<PageResponse<TechnicianPackageSummaryResponse>> =
        ApiResponse.ok(technicianWorkflowService.activePackages(search, Pageables.of(page, limit)))

    @GetMapping("/packages/archive")
    fun archive(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) search: String?,
    ): ApiResponse<PageResponse<TechnicianPackageSummaryResponse>> =
        ApiResponse.ok(technicianWorkflowService.archivePackages(search, Pageables.of(page, limit)))

    @GetMapping("/packages/{id}")
    fun packageDetail(@PathVariable id: Long): ApiResponse<TechnicianPackageDetailResponse> =
        ApiResponse.ok(technicianWorkflowService.packageDetail(id))

    @PostMapping("/packages/{id}/next-status")
    fun nextStatus(
        @PathVariable id: Long,
        @RequestBody(required = false) request: NextStatusRequest?,
    ): ApiResponse<TechnicianPackageDetailResponse> =
        ApiResponse.ok(technicianWorkflowService.nextStatus(id, request ?: NextStatusRequest()))

    @GetMapping("/packages/{id}/modules")
    fun modules(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "50") limit: Int,
    ): ApiResponse<PageResponse<ModuleSummaryResponse>> =
        ApiResponse.ok(moduleService.listModulesForPackage(id, Pageables.of(page, limit)))

    @GetMapping("/packages/{id}/modules/find")
    fun findModule(
        @PathVariable id: Long,
        @RequestParam number: String,
    ): ApiResponse<ModuleSummaryResponse?> =
        ApiResponse.ok(moduleService.findModuleInPackage(id, number))

    @PostMapping("/packages/{id}/modules")
    fun createModule(
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateModuleRequest,
    ): ApiResponse<ModuleDetailResponse> =
        ApiResponse.ok(moduleService.createModule(id, request))

    @GetMapping("/modules/{id}")
    fun moduleDetail(@PathVariable id: Long): ApiResponse<ModuleDetailResponse> =
        ApiResponse.ok(moduleService.technicianModuleDetail(id))

    @PostMapping("/modules/{id}/repair")
    fun repair(
        @PathVariable id: Long,
        @Valid @RequestBody request: RepairRequest,
    ): ApiResponse<ModuleDetailResponse> =
        ApiResponse.ok(moduleService.completeRepair(id, request))

    @PostMapping("/modules/{id}/images")
    fun uploadImages(
        @PathVariable id: Long,
        @RequestParam("files") files: List<MultipartFile>,
    ): ApiResponse<List<ModuleImageResponse>> =
        ApiResponse.ok(imageService.uploadForModule(id, files))

    @GetMapping("/problem-types")
    fun problemTypes(): ApiResponse<List<ProblemTypeResponse>> = ApiResponse.ok(problemTypeService.listActive())

    @GetMapping("/internal-packages")
    fun internalPackages(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) search: String?,
    ): ApiResponse<PageResponse<TechnicianPackageSummaryResponse>> =
        ApiResponse.ok(technicianWorkflowService.listInternalPackages(status, search, Pageables.of(page, limit)))

    @PostMapping("/internal-packages")
    fun createInternalPackage(@Valid @RequestBody request: CreateInternalPackageRequest): ApiResponse<TechnicianPackageDetailResponse> =
        ApiResponse.ok(technicianWorkflowService.createInternalPackage(request))

    @PostMapping("/internal-packages/{id}/confirm-arrival")
    fun confirmArrivalInternal(@PathVariable id: Long): ApiResponse<TechnicianPackageDetailResponse> =
        ApiResponse.ok(technicianWorkflowService.confirmArrivalInternal(id))
}
