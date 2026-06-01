package com.sport360.moduleservice.modules.service

import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.ForbiddenException
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.ValidationException
import com.sport360.moduleservice.images.service.ImageService
import com.sport360.moduleservice.modules.domain.Module
import com.sport360.moduleservice.modules.domain.ModuleRepair
import com.sport360.moduleservice.modules.domain.ModuleStatus
import com.sport360.moduleservice.modules.domain.ModuleStatusHistory
import com.sport360.moduleservice.modules.repository.ModuleRepairRepository
import com.sport360.moduleservice.modules.repository.ModuleRepository
import com.sport360.moduleservice.modules.repository.ModuleStatusHistoryRepository
import com.sport360.moduleservice.modules.repository.ModuleStatusRepository
import com.sport360.moduleservice.modules.web.ClientModuleResponse
import com.sport360.moduleservice.modules.web.CreateModuleRequest
import com.sport360.moduleservice.modules.web.ModuleDetailResponse
import com.sport360.moduleservice.modules.web.ModuleImageResponse
import com.sport360.moduleservice.modules.web.ModuleSummaryResponse
import com.sport360.moduleservice.modules.web.RepairRequest
import com.sport360.moduleservice.packages.domain.PackageStatus
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.packages.repository.PackageStatusRepository
import com.sport360.moduleservice.problemtypes.repository.ProblemTypeRepository
import com.sport360.moduleservice.technicianworkflow.service.TechnicianContext
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/** Module scan/create + repair (technician), and privacy-filtered client module views. */
@Service
class ModuleService(
    private val moduleRepository: ModuleRepository,
    private val moduleStatusRepository: ModuleStatusRepository,
    private val moduleStatusHistoryRepository: ModuleStatusHistoryRepository,
    private val moduleRepairRepository: ModuleRepairRepository,
    private val problemTypeRepository: ProblemTypeRepository,
    private val packageRepository: PackageRepository,
    private val packageStatusRepository: PackageStatusRepository,
    private val technicianContext: TechnicianContext,
    private val auditService: AuditService,
    private val imageService: ImageService,
) {

    // ---- Technician ----

    @Transactional
    fun createModule(packageId: Long, request: CreateModuleRequest): ModuleDetailResponse {
        val technician = technicianContext.current()
        val pkg = packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(packageId, technician.serviceCenter.id)
            ?: throw NotFoundException("Package not found")
        if (pkg.currentStatus.code != STATUS_ON_SERVICE) {
            throw ConflictException("Modules can only be added while the package is on service")
        }
        if (moduleRepository.existsByPackageIdAndModuleNumberAndDeletedAtIsNull(packageId, request.moduleNumber)) {
            throw ConflictException("Module number already exists in this package")
        }
        val problemTypeId = request.problemTypeId ?: throw ValidationException("Problem type is required")
        val problemType = problemTypeRepository.findByIdAndActiveTrue(problemTypeId)
            ?: throw ValidationException("Problem type not found or inactive")
        val waiting = moduleStatus(STATUS_WAITING)
        val module = moduleRepository.save(
            Module(packageId, request.moduleNumber, problemType, waiting, technician, technician.userId),
        )
        moduleStatusHistoryRepository.save(ModuleStatusHistory(module.id, waiting.id, technician.userId))
        auditService.record("module", module.id, "create", technician.userId)
        return technicianDetail(module, null)
    }

    @Transactional(readOnly = true)
    fun listModulesForPackage(packageId: Long, pageable: Pageable): PageResponse<ModuleSummaryResponse> {
        val technician = technicianContext.current()
        packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(packageId, technician.serviceCenter.id)
            ?: throw NotFoundException("Package not found")
        val page = moduleRepository.findAllByPackageIdAndDeletedAtIsNullOrderByCreatedAtAsc(packageId, pageable)
        val thumbnails = imageService.thumbnailUrls(page.content.map { it.id })
        return PageResponse.from(page) { technicianSummary(it, thumbnails[it.id]) }
    }

    @Transactional(readOnly = true)
    fun technicianModuleDetail(moduleId: Long): ModuleDetailResponse {
        val module = scopedModule(moduleId)
        return technicianDetail(module, module.repair)
    }

    /**
     * Looks up a single module by number within a package (service-center scoped), so a technician can
     * jump straight to its repair/view from a scan or typed number. Returns null when no such module
     * exists — the caller offers to create a new service request instead.
     */
    @Transactional(readOnly = true)
    fun findModuleInPackage(packageId: Long, moduleNumber: String): ModuleSummaryResponse? {
        val technician = technicianContext.current()
        packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(packageId, technician.serviceCenter.id)
            ?: throw NotFoundException("Package not found")
        val trimmed = moduleNumber.trim()
        if (trimmed.isEmpty()) return null
        val module = moduleRepository.findFirstByPackageIdAndModuleNumberAndDeletedAtIsNull(packageId, trimmed) ?: return null
        return technicianSummary(module, null)
    }

    @Transactional
    fun completeRepair(moduleId: Long, request: RepairRequest): ModuleDetailResponse {
        val technician = technicianContext.current()
        val module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId) ?: throw NotFoundException("Module not found")
        packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(module.packageId, technician.serviceCenter.id)
            ?: throw NotFoundException("Module not found")
        if (module.currentStatus.code != STATUS_WAITING) {
            throw ConflictException("Module already completed by another technician")
        }
        val repaired = request.decision == "repaired"
        val decisionStatus = moduleStatus(if (repaired) STATUS_REPAIRED else STATUS_NOT_REPAIRABLE)
        val pixels = (if (repaired) request.pixelsRepaired ?: 0 else 0).toShort()
        val chips = (if (repaired) request.chipsReplaced ?: 0 else 0).toShort()
        val price = if (repaired) (request.price ?: BigDecimal.ZERO) else BigDecimal.ZERO
        val repair = moduleRepairRepository.save(
            ModuleRepair(module, technician.userId, decisionStatus, pixels, chips, request.repairNote, price),
        )
        module.currentStatus = decisionStatus
        moduleRepository.save(module)
        moduleStatusHistoryRepository.save(ModuleStatusHistory(module.id, decisionStatus.id, technician.userId))
        auditService.record("repair", module.id, "create", technician.userId)
        return technicianDetail(module, repair)
    }

    // ---- Client (privacy-filtered, status-gated) ----

    @Transactional(readOnly = true)
    fun listClientModules(clientId: Long, packageId: Long, pageable: Pageable): PageResponse<ClientModuleResponse> {
        val pkg = packageRepository.findByIdAndClientIdAndDeletedAtIsNull(packageId, clientId)
            ?: throw NotFoundException("Package not found")
        requireUnlocked(pkg.currentStatus)
        val page = moduleRepository.findAllByPackageIdAndDeletedAtIsNullOrderByCreatedAtAsc(packageId, pageable)
        // Images are shown on the client module detail page, not in the list — keep the list light.
        return PageResponse.from(page) { clientModule(it, emptyList()) }
    }

    @Transactional(readOnly = true)
    fun clientModuleDetail(clientId: Long, moduleId: Long): ClientModuleResponse {
        val module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId) ?: throw NotFoundException("Module not found")
        val pkg = packageRepository.findByIdAndClientIdAndDeletedAtIsNull(module.packageId, clientId)
            ?: throw NotFoundException("Module not found")
        requireUnlocked(pkg.currentStatus)
        return clientModule(module, imageService.imagesForModule(module.id))
    }

    // ---- helpers ----

    private fun scopedModule(moduleId: Long): Module {
        val technician = technicianContext.current()
        val module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId) ?: throw NotFoundException("Module not found")
        packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(module.packageId, technician.serviceCenter.id)
            ?: throw NotFoundException("Module not found")
        return module
    }

    private fun requireUnlocked(status: PackageStatus) {
        val threshold = packageStatusRepository.findByCode(STATUS_REPAIRED_WAITING)?.sortOrder ?: error("status missing")
        if (status.sortOrder < threshold) throw ForbiddenException("Module details are not available yet")
    }

    private fun moduleStatus(code: String): ModuleStatus =
        moduleStatusRepository.findByCode(code) ?: error("Module status '$code' missing")

    private fun technicianSummary(module: Module, thumbnailUrl: String?) = ModuleSummaryResponse(
        id = module.id,
        moduleNumber = module.moduleNumber,
        problemTypeName = module.problemType.name,
        statusCode = module.currentStatus.code,
        statusName = module.currentStatus.name,
        technicianName = module.assignedTechnician.user.name,
        price = module.repair?.price,
        thumbnailUrl = thumbnailUrl,
    )

    private fun technicianDetail(module: Module, repair: ModuleRepair?) = ModuleDetailResponse(
        id = module.id,
        packageId = module.packageId,
        moduleNumber = module.moduleNumber,
        problemTypeName = module.problemType.name,
        statusCode = module.currentStatus.code,
        statusName = module.currentStatus.name,
        technicianName = module.assignedTechnician.user.name,
        pixelsRepaired = repair?.pixelsRepaired?.toInt(),
        chipsReplaced = repair?.chipsReplaced?.toInt(),
        repairNote = repair?.repairNote,
        price = repair?.price,
        decisionStatusCode = repair?.decisionStatus?.code,
        completedAt = repair?.completedAt,
        createdAt = module.createdAt,
        images = imageService.imagesForModule(module.id),
    )

    private fun clientModule(module: Module, images: List<ModuleImageResponse>): ClientModuleResponse {
        val repair = module.repair
        return ClientModuleResponse(
            id = module.id,
            packageId = module.packageId,
            moduleNumber = module.moduleNumber,
            problemTypeName = module.problemType.name,
            statusCode = module.currentStatus.code,
            statusName = module.currentStatus.name,
            pixelsRepaired = repair?.pixelsRepaired?.toInt(),
            chipsReplaced = repair?.chipsReplaced?.toInt(),
            repairNote = repair?.repairNote,
            price = repair?.price,
            images = images,
        )
    }

    private companion object {
        const val STATUS_ON_SERVICE = "on_service"
        const val STATUS_WAITING = "waiting_for_repair"
        const val STATUS_REPAIRED = "repaired"
        const val STATUS_NOT_REPAIRABLE = "not_repairable"
        const val STATUS_REPAIRED_WAITING = "repaired_waiting_shipment"
    }
}
