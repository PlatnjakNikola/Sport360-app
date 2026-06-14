package com.sport360.moduleservice.packages.service

import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.config.AppProperties
import com.sport360.moduleservice.packages.domain.Package
import com.sport360.moduleservice.packages.domain.PackageStatus
import com.sport360.moduleservice.packages.domain.PackageStatusHistory
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.packages.repository.PackageStatusHistoryRepository
import com.sport360.moduleservice.packages.repository.PackageStatusRepository
import com.sport360.moduleservice.packages.web.ActivityItem
import com.sport360.moduleservice.packages.web.ClientDashboardResponse
import com.sport360.moduleservice.packages.web.CreatePackageRequest
import com.sport360.moduleservice.packages.web.PackageDetailResponse
import com.sport360.moduleservice.packages.web.PackageSummaryResponse
import com.sport360.moduleservice.packages.web.StatusCount
import com.sport360.moduleservice.packages.web.TimelineEntry
import com.sport360.moduleservice.packages.web.UpdatePackageRequest
import com.sport360.moduleservice.notifications.service.NotificationService
import com.sport360.moduleservice.servicecenters.repository.ServiceCenterRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * Client-facing package operations. The authenticated user id is the client id
 * (clients.user_id), so every query is scoped to it — cross-client access yields 404.
 */
@Service
class ClientPackageService(
    private val packageRepository: PackageRepository,
    private val packageStatusRepository: PackageStatusRepository,
    private val packageStatusHistoryRepository: PackageStatusHistoryRepository,
    private val serviceCenterRepository: ServiceCenterRepository,
    private val auditService: AuditService,
    private val notificationService: NotificationService,
    private val appProperties: AppProperties,
) {

    @Transactional
    fun createPackage(clientId: Long, request: CreatePackageRequest): PackageDetailResponse {
        if (packageRepository.existsByPackageNumberAndDeletedAtIsNull(request.packageNumber)) {
            throw ConflictException("Package number already in use")
        }
        val created = status(STATUS_CREATED)
        val serviceCenter = resolveDefaultServiceCenter()
        val pkg = packageRepository.save(
            Package(
                request.packageNumber, clientId, serviceCenter.id, created, clientId,
                request.outboundTrackingLink, request.description, request.note, request.approxQuantity,
            ),
        )
        packageStatusHistoryRepository.save(PackageStatusHistory(pkg.id, created.id, clientId))
        auditService.record("package", pkg.id, "create", clientId)
        notificationService.notifyAdmin(
            "package_created", "New package created", "Package ${pkg.packageNumber} was created by a client.", "package", pkg.id,
        )
        return toDetail(pkg)
    }

    @Transactional(readOnly = true)
    fun listPackages(clientId: Long, statusCode: String?, search: String?, pageable: Pageable): PageResponse<PackageSummaryResponse> {
        val page = packageRepository.findClientPackages(
            clientId,
            statusCode?.takeIf { it.isNotBlank() },
            search?.takeIf { it.isNotBlank() },
            pageable,
        )
        return PageResponse.from(page) { it.toSummary() }
    }

    @Transactional(readOnly = true)
    fun getPackage(clientId: Long, packageId: Long): PackageDetailResponse = toDetail(ownPackage(clientId, packageId))

    @Transactional
    fun updatePackage(clientId: Long, packageId: Long, request: UpdatePackageRequest): PackageDetailResponse {
        val pkg = ownPackage(clientId, packageId)
        request.outboundTrackingLink?.let { pkg.outboundTrackingLink = it }
        request.note?.let { pkg.note = it }
        request.description?.let { pkg.description = it }
        packageRepository.save(pkg)
        auditService.record("package", pkg.id, "update", clientId)
        return toDetail(pkg)
    }

    @Transactional
    fun markSent(clientId: Long, packageId: Long): PackageDetailResponse {
        val pkg = ownPackage(clientId, packageId)
        if (pkg.currentStatus.code != STATUS_CREATED) {
            throw ConflictException("Package can only be sent from the 'created' state")
        }
        val sent = status(STATUS_SENT)
        pkg.currentStatus = sent
        packageRepository.save(pkg)
        packageStatusHistoryRepository.save(PackageStatusHistory(pkg.id, sent.id, clientId))
        auditService.record("package", pkg.id, "status_change", clientId)
        notificationService.notifyAdmin(
            "status_change", "Package sent to service", "Package ${pkg.packageNumber} was sent to the service.", "package", pkg.id,
        )
        notificationService.notifyServiceCenterTechnicians(
            pkg.serviceCenterId, "package_incoming", "New package incoming",
            "Package ${pkg.packageNumber} is on its way to your service center.", "package", pkg.id,
        )
        return toDetail(pkg)
    }

    @Transactional
    fun confirmArrival(clientId: Long, packageId: Long): PackageDetailResponse {
        val pkg = ownPackage(clientId, packageId)
        if (pkg.currentStatus.code != STATUS_SHIPPED) {
            throw ConflictException("Package can only be confirmed once it has been shipped")
        }
        val arrived = status(STATUS_ARRIVED)
        pkg.currentStatus = arrived
        pkg.arrivedAt = OffsetDateTime.now()
        packageRepository.save(pkg)
        packageStatusHistoryRepository.save(PackageStatusHistory(pkg.id, arrived.id, clientId))
        auditService.record("package", pkg.id, "status_change", clientId)
        notificationService.notifyAdmin(
            "status_change", "Client confirmed arrival", "Package ${pkg.packageNumber} was confirmed as arrived.", "package", pkg.id,
        )
        notificationService.notifyServiceCenterTechnicians(
            pkg.serviceCenterId, "status_change", "Package ${pkg.packageNumber}: Arrived",
            "Package ${pkg.packageNumber} has arrived at its destination.", "package", pkg.id,
        )
        return toDetail(pkg)
    }

    @Transactional(readOnly = true)
    fun dashboard(clientId: Long): ClientDashboardResponse {
        val counts = packageRepository.countByStatusForClient(clientId).map { StatusCount(it.code, it.name, it.count) }
        val activity = packageStatusHistoryRepository.recentActivityForClient(clientId, PageRequest.of(0, 10))
            .map { ActivityItem(it.packageId, it.packageNumber, it.statusCode, it.statusName, it.changedAt) }
        return ClientDashboardResponse(counts, activity)
    }

    private fun ownPackage(clientId: Long, packageId: Long): Package =
        packageRepository.findByIdAndClientIdAndDeletedAtIsNull(packageId, clientId)
            ?: throw NotFoundException("Package not found")

    private fun status(code: String): PackageStatus =
        packageStatusRepository.findByCode(code) ?: error("Package status '$code' missing")

    private fun resolveDefaultServiceCenter() =
        serviceCenterRepository.findByCode(appProperties.defaultServiceCenterCode)
            ?: serviceCenterRepository.findAllByActiveTrueOrderByNameAsc().firstOrNull()
            ?: error("No active service center configured")

    private fun Package.toSummary() = PackageSummaryResponse(
        id = id,
        packageNumber = packageNumber,
        statusCode = currentStatus.code,
        statusName = currentStatus.name,
        outboundTrackingLink = outboundTrackingLink,
        returnTrackingLink = returnTrackingLink,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun toDetail(pkg: Package) = PackageDetailResponse(
        id = pkg.id,
        packageNumber = pkg.packageNumber,
        statusCode = pkg.currentStatus.code,
        statusName = pkg.currentStatus.name,
        outboundTrackingLink = pkg.outboundTrackingLink,
        returnTrackingLink = pkg.returnTrackingLink,
        description = pkg.description,
        note = pkg.note,
        approxQuantity = pkg.approxQuantity,
        createdAt = pkg.createdAt,
        receivedAt = pkg.receivedAt,
        serviceCompletedAt = pkg.serviceCompletedAt,
        shippedAt = pkg.shippedAt,
        arrivedAt = pkg.arrivedAt,
        timeline = packageStatusHistoryRepository.timeline(pkg.id)
            .map { TimelineEntry(it.statusCode, it.statusName, it.changedAt) },
    )

    private companion object {
        const val STATUS_CREATED = "created"
        const val STATUS_SENT = "sent_to_service"
        const val STATUS_SHIPPED = "shipped_to_client"
        const val STATUS_ARRIVED = "arrived"
    }
}
