package com.sport360.moduleservice.technicianworkflow.service

import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.modules.repository.ModuleRepairRepository
import com.sport360.moduleservice.modules.repository.ModuleRepository
import com.sport360.moduleservice.notifications.service.NotificationService
import com.sport360.moduleservice.packages.domain.Package
import com.sport360.moduleservice.packages.domain.PackageStatusHistory
import com.sport360.moduleservice.packages.domain.VPackageSummary
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.packages.repository.PackageStatusHistoryRepository
import com.sport360.moduleservice.packages.repository.PackageStatusRepository
import com.sport360.moduleservice.packages.repository.VPackageSummaryRepository
import com.sport360.moduleservice.packages.web.ActivityItem
import com.sport360.moduleservice.packages.web.StatusCount
import com.sport360.moduleservice.packages.web.TimelineEntry
import com.sport360.moduleservice.technicianworkflow.web.CreateInternalPackageRequest
import com.sport360.moduleservice.technicianworkflow.web.NextStatusRequest
import com.sport360.moduleservice.technicianworkflow.web.PersonalStats
import com.sport360.moduleservice.technicianworkflow.web.TechnicianDashboardResponse
import com.sport360.moduleservice.technicianworkflow.web.TechnicianPackageDetailResponse
import com.sport360.moduleservice.technicianworkflow.web.TechnicianPackageSummaryResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/** Technician package workflow: dashboard, active/archive lists, detail, forward-only next-status. */
@Service
class TechnicianWorkflowService(
    private val vPackageSummaryRepository: VPackageSummaryRepository,
    private val packageRepository: PackageRepository,
    private val packageStatusRepository: PackageStatusRepository,
    private val packageStatusHistoryRepository: PackageStatusHistoryRepository,
    private val moduleRepository: ModuleRepository,
    private val moduleRepairRepository: ModuleRepairRepository,
    private val technicianContext: TechnicianContext,
    private val auditService: AuditService,
    private val notificationService: NotificationService,
) {

    @Transactional(readOnly = true)
    fun activePackages(search: String?, pageable: Pageable): PageResponse<TechnicianPackageSummaryResponse> {
        val page = vPackageSummaryRepository.findForServiceCenter(
            technicianContext.serviceCenterId(), ACTIVE_STATUSES, search?.takeIf { it.isNotBlank() }, pageable,
        )
        return PageResponse.from(page) { it.toSummary() }
    }

    @Transactional(readOnly = true)
    fun archivePackages(search: String?, pageable: Pageable): PageResponse<TechnicianPackageSummaryResponse> {
        val page = vPackageSummaryRepository.findForServiceCenter(
            technicianContext.serviceCenterId(), ARCHIVE_STATUSES, search?.takeIf { it.isNotBlank() }, pageable,
        )
        return PageResponse.from(page) { it.toSummary() }
    }

    @Transactional(readOnly = true)
    fun packageDetail(packageId: Long): TechnicianPackageDetailResponse {
        val pkg = packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(packageId, technicianContext.serviceCenterId())
            ?: throw NotFoundException("Package not found")
        return buildDetail(pkg)
    }

    @Transactional
    fun nextStatus(packageId: Long, request: NextStatusRequest): TechnicianPackageDetailResponse {
        val technician = technicianContext.current()
        val pkg = packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(packageId, technician.serviceCenter.id)
            ?: throw NotFoundException("Package not found")
        val currentOrder = pkg.currentStatus.sortOrder.toInt()
        // Internal packages have no client, so the technician also drives created → sent_to_service.
        val minOrder = if (pkg.isInternal) ORDER_CREATED else ORDER_SENT
        if (currentOrder < minOrder || currentOrder >= ORDER_SHIPPED) {
            throw ConflictException("This package cannot be advanced by a technician")
        }
        val next = packageStatusRepository.findBySortOrder((currentOrder + 1).toShort()) ?: error("Next status missing")
        val now = OffsetDateTime.now()
        when (next.code) {
            "received_by_service" -> pkg.receivedAt = now
            "on_service" -> pkg.serviceStartedAt = now
            "repaired_waiting_shipment" -> {
                if (moduleRepository.countActive(packageId) == 0L) {
                    throw ConflictException("Add at least one module before finishing service")
                }
                if (moduleRepository.countUnfinished(packageId) > 0L) {
                    throw ConflictException("All modules must be completed before finishing service")
                }
                pkg.serviceCompletedAt = now
            }
            "shipped_to_client" -> {
                pkg.shippedAt = now
                request.returnTrackingLink?.takeIf { it.isNotBlank() }?.let { pkg.returnTrackingLink = it }
            }
        }
        pkg.currentStatus = next
        packageRepository.save(pkg)
        packageStatusHistoryRepository.save(PackageStatusHistory(pkg.id, next.id, technician.userId))
        auditService.record("package", pkg.id, "status_change", technician.userId)
        notifyStatusChange(pkg, next.name)
        return buildDetail(pkg)
    }

    @Transactional
    fun createInternalPackage(request: CreateInternalPackageRequest): TechnicianPackageDetailResponse {
        val technician = technicianContext.current()
        if (packageRepository.existsByPackageNumberAndDeletedAtIsNull(request.packageNumber)) {
            throw ConflictException("Package number already in use")
        }
        val created = packageStatusRepository.findByCode("created") ?: error("Package status 'created' missing")
        val pkg = packageRepository.save(
            Package.internal(
                request.packageNumber, technician.serviceCenter.id, created, technician.userId,
                request.description, request.note, request.approxQuantity,
            ),
        )
        packageStatusHistoryRepository.save(PackageStatusHistory(pkg.id, created.id, technician.userId))
        auditService.record("package", pkg.id, "create", technician.userId)
        return buildDetail(pkg)
    }

    @Transactional(readOnly = true)
    fun listInternalPackages(statusCode: String?, search: String?, pageable: Pageable): PageResponse<TechnicianPackageSummaryResponse> {
        val page = vPackageSummaryRepository.findInternalForServiceCenter(
            technicianContext.serviceCenterId(), statusCode?.takeIf { it.isNotBlank() }, search?.takeIf { it.isNotBlank() }, pageable,
        )
        return PageResponse.from(page) { it.toSummary() }
    }

    @Transactional
    fun confirmArrivalInternal(packageId: Long): TechnicianPackageDetailResponse {
        val technician = technicianContext.current()
        val pkg = packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(packageId, technician.serviceCenter.id)
            ?: throw NotFoundException("Package not found")
        if (!pkg.isInternal) throw ConflictException("Only internal packages can be confirmed by a technician")
        if (pkg.currentStatus.code != "shipped_to_client") {
            throw ConflictException("Package can only be confirmed once it has been shipped")
        }
        val arrived = packageStatusRepository.findByCode("arrived") ?: error("Package status 'arrived' missing")
        pkg.currentStatus = arrived
        pkg.arrivedAt = OffsetDateTime.now()
        packageRepository.save(pkg)
        packageStatusHistoryRepository.save(PackageStatusHistory(pkg.id, arrived.id, technician.userId))
        auditService.record("package", pkg.id, "status_change", technician.userId)
        notificationService.notifyAdmin(
            "status_change", "Internal package arrived", "Internal package ${pkg.packageNumber} was confirmed as arrived.", "package", pkg.id,
        )
        notificationService.notifyServiceCenterTechnicians(
            pkg.serviceCenterId, "status_change", "Package ${pkg.packageNumber}: Arrived",
            "Internal package ${pkg.packageNumber} has arrived.", "package", pkg.id, exceptUserId = technician.userId,
        )
        return buildDetail(pkg)
    }

    @Transactional(readOnly = true)
    fun dashboard(): TechnicianDashboardResponse {
        val technician = technicianContext.current()
        val serviceCenterId = technician.serviceCenter.id
        val counts = vPackageSummaryRepository.countByStatusForServiceCenter(serviceCenterId, ACTIVE_STATUSES)
            .map { StatusCount(it.code, it.name, it.count) }

        val now = OffsetDateTime.now()
        val startOfDay = now.truncatedTo(ChronoUnit.DAYS)
        val weekAgo = now.minusDays(7)
        val totalRepairs = moduleRepairRepository.countByTechnicianId(technician.userId)
        val repaired = moduleRepairRepository.countRepairedByTechnician(technician.userId)
        val personalStats = PersonalStats(
            modulesToday = moduleRepairRepository.countByTechnicianIdAndCompletedAtAfter(technician.userId, startOfDay),
            modulesThisWeek = moduleRepairRepository.countByTechnicianIdAndCompletedAtAfter(technician.userId, weekAgo),
            repairRatePercent = if (totalRepairs > 0) ((repaired * 100) / totalRepairs).toInt() else 0,
            totalValueThisWeek = moduleRepairRepository.sumValueByTechnicianSince(technician.userId, weekAgo),
        )

        val activity = packageStatusHistoryRepository.recentActivityForServiceCenter(serviceCenterId, PageRequest.of(0, 10))
            .map { ActivityItem(it.packageId, it.packageNumber, it.statusCode, it.statusName, it.changedAt) }

        return TechnicianDashboardResponse(counts, personalStats, activity)
    }

    /** Notifies the admin always, and the client too for external packages. */
    private fun notifyStatusChange(pkg: Package, statusName: String) {
        val title = "Package ${pkg.packageNumber}: $statusName"
        notificationService.notifyAdmin("status_change", title, null, "package", pkg.id)
        if (!pkg.isInternal) {
            pkg.clientId?.let { clientId ->
                notificationService.notify(clientId, "status_change", title, "Your package is now \"$statusName\".", "package", pkg.id)
            }
        }
    }

    private fun buildDetail(pkg: Package): TechnicianPackageDetailResponse {
        val total = moduleRepository.countActive(pkg.id)
        val finished = total - moduleRepository.countUnfinished(pkg.id)
        val timeline = packageStatusHistoryRepository.timeline(pkg.id)
            .map { TimelineEntry(it.statusCode, it.statusName, it.changedAt) }
        return TechnicianPackageDetailResponse(
            id = pkg.id,
            packageNumber = pkg.packageNumber,
            statusCode = pkg.currentStatus.code,
            statusName = pkg.currentStatus.name,
            isInternal = pkg.isInternal,
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
            totalModules = total,
            finishedModules = finished,
            timeline = timeline,
        )
    }

    private fun VPackageSummary.toSummary() = TechnicianPackageSummaryResponse(
        id = id,
        packageNumber = packageNumber,
        statusCode = statusCode,
        statusName = statusName,
        isInternal = isInternal,
        createdAt = createdAt,
        receivedAt = receivedAt,
        totalModules = totalModules,
        finishedModules = finishedModules,
        totalValue = totalValue,
    )

    private companion object {
        val ACTIVE_STATUSES = listOf("sent_to_service", "received_by_service", "on_service", "repaired_waiting_shipment")
        val ARCHIVE_STATUSES = listOf("shipped_to_client", "arrived")
        const val ORDER_CREATED = 1
        const val ORDER_SENT = 2
        const val ORDER_SHIPPED = 6
    }
}
