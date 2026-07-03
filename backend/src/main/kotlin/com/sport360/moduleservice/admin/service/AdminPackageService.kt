package com.sport360.moduleservice.admin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.admin.web.AdminCreateModuleRequest
import com.sport360.moduleservice.admin.web.AdminModuleDetailResponse
import com.sport360.moduleservice.admin.web.AdminModuleSummaryResponse
import com.sport360.moduleservice.admin.web.AdminPackageDetailResponse
import com.sport360.moduleservice.admin.web.AdminPackageSummaryResponse
import com.sport360.moduleservice.admin.web.AdminUpdatePackageRequest
import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.ValidationException
import com.sport360.moduleservice.images.service.ImageService
import com.sport360.moduleservice.modules.domain.Module
import com.sport360.moduleservice.modules.domain.ModuleStatusHistory
import com.sport360.moduleservice.modules.repository.ModuleRepository
import com.sport360.moduleservice.modules.repository.ModuleStatusHistoryRepository
import com.sport360.moduleservice.modules.repository.ModuleStatusRepository
import com.sport360.moduleservice.notifications.service.NotificationService
import com.sport360.moduleservice.packages.domain.Package
import com.sport360.moduleservice.packages.domain.PackageStatus
import com.sport360.moduleservice.packages.domain.PackageStatusHistory
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.packages.repository.PackageStatusHistoryRepository
import com.sport360.moduleservice.packages.repository.PackageStatusRepository
import com.sport360.moduleservice.packages.repository.VAdminPackageSummaryRepository
import com.sport360.moduleservice.packages.web.TimelineEntry
import com.sport360.moduleservice.problemtypes.repository.ProblemTypeRepository
import com.sport360.moduleservice.security.CurrentUserService
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Admin package control: list (incl. deleted), detail, edit, status override, soft delete/restore, bulk, correction. */
@Service
class AdminPackageService(
    private val adminSummaryRepository: VAdminPackageSummaryRepository,
    private val packageRepository: PackageRepository,
    private val packageStatusRepository: PackageStatusRepository,
    private val packageStatusHistoryRepository: PackageStatusHistoryRepository,
    private val moduleRepository: ModuleRepository,
    private val moduleStatusRepository: ModuleStatusRepository,
    private val moduleStatusHistoryRepository: ModuleStatusHistoryRepository,
    private val problemTypeRepository: ProblemTypeRepository,
    private val technicianRepository: TechnicianRepository,
    private val adminModuleService: AdminModuleService,
    private val imageService: ImageService,
    private val currentUserService: CurrentUserService,
    private val auditService: AuditService,
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper,
) {

    @Transactional(readOnly = true)
    fun list(
        includeDeleted: Boolean,
        statusCode: String?,
        clientId: Long?,
        serviceCenterId: Long?,
        type: String?,
        search: String?,
        dateFrom: OffsetDateTime?,
        dateTo: OffsetDateTime?,
        pageable: Pageable,
    ): PageResponse<AdminPackageSummaryResponse> {
        val page = adminSummaryRepository.search(
            includeDeleted, statusCode?.takeIf { it.isNotBlank() }, clientId, serviceCenterId,
            type?.takeIf { it.isNotBlank() && it != "all" }, search?.takeIf { it.isNotBlank() },
            dateFrom ?: DATE_FLOOR, dateTo ?: DATE_CEILING, pageable,
        )
        return PageResponse.from(page) {
            AdminPackageSummaryResponse(
                id = it.id,
                packageNumber = it.packageNumber,
                companyName = it.companyName,
                isInternal = it.isInternal,
                statusCode = it.statusCode,
                statusName = it.statusName,
                serviceCenterId = it.serviceCenterId,
                createdAt = it.createdAt,
                receivedAt = it.receivedAt,
                shippedAt = it.shippedAt,
                deleted = it.deletedAt != null,
                totalModules = it.totalModules,
                repairedCount = it.repairedCount,
                notRepairableCount = it.notRepairableCount,
                totalValue = it.totalValue,
            )
        }
    }

    @Transactional(readOnly = true)
    fun detail(packageId: Long): AdminPackageDetailResponse = buildDetail(loadPackage(packageId))

    @Transactional
    fun update(packageId: Long, request: AdminUpdatePackageRequest): AdminPackageDetailResponse {
        val pkg = loadPackage(packageId)
        val before = mapOf(
            "packageNumber" to pkg.packageNumber, "description" to pkg.description, "note" to pkg.note,
            "outboundTrackingLink" to pkg.outboundTrackingLink, "returnTrackingLink" to pkg.returnTrackingLink,
        )
        request.packageNumber?.takeIf { it.isNotBlank() && it != pkg.packageNumber }?.let { newNumber ->
            if (packageRepository.existsByPackageNumberAndDeletedAtIsNull(newNumber)) {
                throw ConflictException("Package number already in use")
            }
            pkg.packageNumber = newNumber
        }
        request.description?.let { pkg.description = it }
        request.note?.let { pkg.note = it }
        request.outboundTrackingLink?.let { pkg.outboundTrackingLink = it }
        request.returnTrackingLink?.let { pkg.returnTrackingLink = it }
        packageRepository.save(pkg)
        audit(pkg.id, "update", before, pkg)
        return buildDetail(pkg)
    }

    @Transactional
    fun overrideStatus(packageId: Long, statusId: Short): AdminPackageDetailResponse {
        val pkg = loadPackage(packageId)
        applyStatus(pkg, status(statusId))
        return buildDetail(pkg)
    }

    @Transactional
    fun softDelete(packageId: Long) {
        val pkg = loadPackage(packageId)
        if (pkg.deletedAt == null) {
            pkg.deletedAt = OffsetDateTime.now()
            packageRepository.save(pkg)
            auditService.record("package", pkg.id, "delete", currentUserService.currentUserId())
        }
    }

    @Transactional
    fun restore(packageId: Long): AdminPackageDetailResponse {
        val pkg = loadPackage(packageId)
        if (pkg.deletedAt == null) throw ConflictException("Package is not deleted")
        if (packageRepository.existsByPackageNumberAndDeletedAtIsNull(pkg.packageNumber)) {
            throw ConflictException("Package number already in use — cannot restore")
        }
        pkg.deletedAt = null
        packageRepository.save(pkg)
        auditService.record("package", pkg.id, "update", currentUserService.currentUserId())
        return buildDetail(pkg)
    }

    @Transactional
    fun bulkDelete(packageIds: List<Long>): Int {
        val packages = packageRepository.findAllById(packageIds)
        if (packages.size != packageIds.toSet().size) throw NotFoundException("One or more packages were not found")
        val userId = currentUserService.currentUserId()
        val now = OffsetDateTime.now()
        var affected = 0
        packages.forEach { pkg ->
            if (pkg.deletedAt == null) {
                pkg.deletedAt = now
                packageRepository.save(pkg)
                auditService.record("package", pkg.id, "delete", userId)
                affected++
            }
        }
        return affected
    }

    @Transactional
    fun bulkStatus(packageIds: List<Long>, statusId: Short): Int {
        val target = status(statusId)
        val packages = packageRepository.findAllById(packageIds)
        if (packages.size != packageIds.toSet().size) throw NotFoundException("One or more packages were not found")
        packages.forEach { applyStatus(it, target) }
        return packages.size
    }

    @Transactional
    fun addCorrectionModule(packageId: Long, request: AdminCreateModuleRequest): AdminModuleDetailResponse {
        val pkg = packageRepository.findByIdAndDeletedAtIsNull(packageId)
            ?: throw NotFoundException("Package not found")
        if (moduleRepository.existsByPackageIdAndModuleNumberAndDeletedAtIsNull(packageId, request.moduleNumber)) {
            throw ConflictException("Module number already exists in this package")
        }
        val problemType = problemTypeRepository.findById(request.problemTypeId)
            .orElseThrow { ValidationException("Problem type not found") }
        val technician = technicianRepository.findById(request.assignedTechnicianId)
            .orElseThrow { ValidationException("Technician not found") }
        if (!technician.user.isActive) throw ValidationException("Technician is not active")
        val waiting = moduleStatusRepository.findByCode("waiting_for_repair") ?: error("module status missing")
        val module = moduleRepository.save(
            Module(pkg.id, request.moduleNumber, problemType, waiting, technician, currentUserService.currentUserId()),
        )
        moduleStatusHistoryRepository.save(ModuleStatusHistory(module.id, waiting.id, currentUserService.currentUserId()))
        auditService.record(
            "module", module.id, "create", currentUserService.currentUserId(),
            newValueJson = """{"adminCorrection":true,"packageId":${pkg.id}}""",
        )
        return adminModuleService.detail(module.id)
    }

    // ---- helpers ----

    private fun loadPackage(packageId: Long): Package =
        packageRepository.findById(packageId).orElseThrow { NotFoundException("Package not found") }

    private fun status(statusId: Short): PackageStatus =
        packageStatusRepository.findById(statusId).orElseThrow { ValidationException("Invalid status") }

    private companion object {
        // Wide bounds so the optional date filter can always bind a typed timestamp parameter.
        val DATE_FLOOR: OffsetDateTime = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val DATE_CEILING: OffsetDateTime = OffsetDateTime.of(2200, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    }

    /** Applies an admin status override: sets the status + its denormalized date, history, audit. */
    private fun applyStatus(pkg: Package, target: PackageStatus) {
        val now = OffsetDateTime.now()
        when (target.code) {
            "received_by_service" -> if (pkg.receivedAt == null) pkg.receivedAt = now
            "on_service" -> if (pkg.serviceStartedAt == null) pkg.serviceStartedAt = now
            "repaired_waiting_shipment" -> if (pkg.serviceCompletedAt == null) pkg.serviceCompletedAt = now
            "shipped_to_client" -> if (pkg.shippedAt == null) pkg.shippedAt = now
            "arrived" -> if (pkg.arrivedAt == null) pkg.arrivedAt = now
        }
        pkg.currentStatus = target
        packageRepository.save(pkg)
        packageStatusHistoryRepository.save(PackageStatusHistory(pkg.id, target.id, currentUserService.currentUserId()))
        auditService.record("package", pkg.id, "admin_override", currentUserService.currentUserId())
        if (!pkg.isInternal) {
            pkg.clientId?.let { clientId ->
                notificationService.notify(
                    clientId, "status_change", "Package ${pkg.packageNumber}: ${target.name}",
                    "Your package is now \"${target.name}\".", "package", pkg.id,
                )
            }
        }
    }

    private fun audit(packageId: Long, action: String, before: Map<String, Any?>, pkg: Package) {
        val after = mapOf(
            "packageNumber" to pkg.packageNumber, "description" to pkg.description, "note" to pkg.note,
            "outboundTrackingLink" to pkg.outboundTrackingLink, "returnTrackingLink" to pkg.returnTrackingLink,
        )
        auditService.record(
            "package", packageId, action, currentUserService.currentUserId(),
            oldValueJson = objectMapper.writeValueAsString(before),
            newValueJson = objectMapper.writeValueAsString(after),
        )
    }

    private fun buildDetail(pkg: Package): AdminPackageDetailResponse {
        val summary = adminSummaryRepository.findById(pkg.id).orElse(null)
        val repaired = summary?.repairedCount ?: 0
        val totalValue = summary?.totalValue ?: BigDecimal.ZERO
        val averagePrice = if (repaired > 0) totalValue.divide(BigDecimal(repaired), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val modules = moduleRepository.findAllByPackageIdAndDeletedAtIsNullOrderByCreatedAtAsc(pkg.id)
        val thumbnails = imageService.thumbnailUrls(modules.map { it.id })
        val timeline = packageStatusHistoryRepository.timeline(pkg.id)
            .map { TimelineEntry(it.statusCode, it.statusName, it.changedAt) }
        return AdminPackageDetailResponse(
            id = pkg.id,
            packageNumber = pkg.packageNumber,
            clientId = pkg.clientId,
            companyName = summary?.companyName,
            isInternal = pkg.isInternal,
            serviceCenterId = pkg.serviceCenterId,
            statusCode = pkg.currentStatus.code,
            statusName = pkg.currentStatus.name,
            outboundTrackingLink = pkg.outboundTrackingLink,
            returnTrackingLink = pkg.returnTrackingLink,
            description = pkg.description,
            note = pkg.note,
            approxQuantity = pkg.approxQuantity,
            createdAt = pkg.createdAt,
            receivedAt = pkg.receivedAt,
            serviceStartedAt = pkg.serviceStartedAt,
            serviceCompletedAt = pkg.serviceCompletedAt,
            shippedAt = pkg.shippedAt,
            arrivedAt = pkg.arrivedAt,
            deletedAt = pkg.deletedAt,
            totalModules = summary?.totalModules ?: 0,
            repairedCount = repaired,
            notRepairableCount = summary?.notRepairableCount ?: 0,
            totalValue = totalValue,
            averagePrice = averagePrice,
            timeline = timeline,
            modules = modules.map {
                AdminModuleSummaryResponse(
                    id = it.id,
                    moduleNumber = it.moduleNumber,
                    problemTypeName = it.problemType.name,
                    statusCode = it.currentStatus.code,
                    statusName = it.currentStatus.name,
                    technicianName = it.assignedTechnician.user.name,
                    price = it.repair?.price,
                    thumbnailUrl = thumbnails[it.id],
                )
            },
        )
    }
}
