package com.sport360.moduleservice.admin.service

import com.sport360.moduleservice.admin.web.AdminModuleDetailResponse
import com.sport360.moduleservice.admin.web.AdminUpdateModuleRequest
import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.NotFoundException
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
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.problemtypes.repository.ProblemTypeRepository
import com.sport360.moduleservice.security.CurrentUserService
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/** Admin module control: full detail, edit (data/technician/status/repair), soft delete/restore. */
@Service
class AdminModuleService(
    private val moduleRepository: ModuleRepository,
    private val moduleStatusRepository: ModuleStatusRepository,
    private val moduleStatusHistoryRepository: ModuleStatusHistoryRepository,
    private val moduleRepairRepository: ModuleRepairRepository,
    private val problemTypeRepository: ProblemTypeRepository,
    private val technicianRepository: TechnicianRepository,
    private val packageRepository: PackageRepository,
    private val imageService: ImageService,
    private val currentUserService: CurrentUserService,
    private val auditService: AuditService,
) {

    @Transactional(readOnly = true)
    fun detail(moduleId: Long): AdminModuleDetailResponse = buildDetail(loadModule(moduleId))

    @Transactional
    fun update(moduleId: Long, request: AdminUpdateModuleRequest): AdminModuleDetailResponse {
        val module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId) ?: throw NotFoundException("Module not found")
        request.problemTypeId?.let { id ->
            module.problemType = problemTypeRepository.findById(id).orElseThrow { ValidationException("Problem type not found") }
        }
        request.assignedTechnicianId?.let { id ->
            module.assignedTechnician = technicianRepository.findById(id).orElseThrow { ValidationException("Technician not found") }
        }
        var statusChanged = false
        if (request.statusCode != null) {
            val target = moduleStatusRepository.findByCode(request.statusCode) ?: throw ValidationException("Invalid module status")
            module.currentStatus = target
            statusChanged = true
            if (target.code == "repaired" || target.code == "not_repairable") {
                upsertRepair(module, target, request, repaired = target.code == "repaired")
            }
        } else {
            module.repair?.let { applyRepairEdits(it, request) }
        }
        moduleRepository.save(module)
        if (statusChanged) {
            moduleStatusHistoryRepository.save(ModuleStatusHistory(module.id, module.currentStatus.id, currentUserService.currentUserId()))
        }
        auditService.record("module", module.id, "update", currentUserService.currentUserId())
        return buildDetail(module)
    }

    @Transactional
    fun softDelete(moduleId: Long) {
        val module = loadModule(moduleId)
        if (module.deletedAt == null) {
            module.deletedAt = java.time.OffsetDateTime.now()
            moduleRepository.save(module)
            auditService.record("module", module.id, "delete", currentUserService.currentUserId())
        }
    }

    @Transactional
    fun restore(moduleId: Long): AdminModuleDetailResponse {
        val module = loadModule(moduleId)
        if (module.deletedAt == null) throw ConflictException("Module is not deleted")
        if (moduleRepository.existsByPackageIdAndModuleNumberAndDeletedAtIsNull(module.packageId, module.moduleNumber)) {
            throw ConflictException("Module number already in use in this package — cannot restore")
        }
        module.deletedAt = null
        moduleRepository.save(module)
        auditService.record("module", module.id, "update", currentUserService.currentUserId())
        return buildDetail(module)
    }

    // ---- helpers ----

    private fun loadModule(moduleId: Long): Module =
        moduleRepository.findById(moduleId).orElseThrow { NotFoundException("Module not found") }

    private fun upsertRepair(module: Module, decisionStatus: ModuleStatus, request: AdminUpdateModuleRequest, repaired: Boolean) {
        val existing = module.repair
        val pixels = (if (repaired) request.pixelsRepaired ?: existing?.pixelsRepaired?.toInt() ?: 0 else 0).toShort()
        val chips = (if (repaired) request.chipsReplaced ?: existing?.chipsReplaced?.toInt() ?: 0 else 0).toShort()
        val price = if (repaired) (request.price ?: existing?.price ?: BigDecimal.ZERO) else BigDecimal.ZERO
        val note = request.repairNote ?: existing?.repairNote
        if (existing == null) {
            moduleRepairRepository.save(
                ModuleRepair(module, module.assignedTechnician.userId, decisionStatus, pixels, chips, note, price),
            )
        } else {
            existing.decisionStatus = decisionStatus
            existing.pixelsRepaired = pixels
            existing.chipsReplaced = chips
            existing.price = price
            existing.repairNote = note
            moduleRepairRepository.save(existing)
        }
    }

    private fun applyRepairEdits(repair: ModuleRepair, request: AdminUpdateModuleRequest) {
        request.pixelsRepaired?.let { repair.pixelsRepaired = it.toShort() }
        request.chipsReplaced?.let { repair.chipsReplaced = it.toShort() }
        request.price?.let { repair.price = it }
        request.repairNote?.let { repair.repairNote = it }
        moduleRepairRepository.save(repair)
    }

    private fun buildDetail(module: Module): AdminModuleDetailResponse {
        val pkg = packageRepository.findById(module.packageId).orElse(null)
        val repair = module.repair
        return AdminModuleDetailResponse(
            id = module.id,
            packageId = module.packageId,
            packageNumber = pkg?.packageNumber ?: "",
            moduleNumber = module.moduleNumber,
            problemTypeId = module.problemType.id,
            problemTypeName = module.problemType.name,
            statusCode = module.currentStatus.code,
            statusName = module.currentStatus.name,
            technicianId = module.assignedTechnician.userId,
            technicianName = module.assignedTechnician.user.name,
            pixelsRepaired = repair?.pixelsRepaired?.toInt(),
            chipsReplaced = repair?.chipsReplaced?.toInt(),
            repairNote = repair?.repairNote,
            price = repair?.price,
            decisionStatusCode = repair?.decisionStatus?.code,
            createdAt = module.createdAt,
            completedAt = repair?.completedAt,
            deletedAt = module.deletedAt,
            images = imageService.imagesForModule(module.id),
        )
    }
}
