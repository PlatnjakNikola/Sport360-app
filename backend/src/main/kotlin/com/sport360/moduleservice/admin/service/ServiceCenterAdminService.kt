package com.sport360.moduleservice.admin.service

import com.sport360.moduleservice.admin.web.CreateServiceCenterRequest
import com.sport360.moduleservice.admin.web.ServiceCenterAdminResponse
import com.sport360.moduleservice.admin.web.UpdateServiceCenterRequest
import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.security.CurrentUserService
import com.sport360.moduleservice.servicecenters.domain.ServiceCenter
import com.sport360.moduleservice.servicecenters.repository.ServiceCenterRepository
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Admin CRUD for service centers. Deactivation is blocked while active technicians are assigned. */
@Service
class ServiceCenterAdminService(
    private val serviceCenterRepository: ServiceCenterRepository,
    private val technicianRepository: TechnicianRepository,
    private val currentUserService: CurrentUserService,
    private val auditService: AuditService,
) {

    @Transactional(readOnly = true)
    fun list(): List<ServiceCenterAdminResponse> =
        serviceCenterRepository.findAllByOrderByNameAsc().map { it.toResponse() }

    @Transactional
    fun create(request: CreateServiceCenterRequest): ServiceCenterAdminResponse {
        if (serviceCenterRepository.existsByCode(request.code)) throw ConflictException("Code already in use")
        val saved = serviceCenterRepository.save(
            ServiceCenter(request.code, request.name, request.country, request.city, request.address),
        )
        auditService.record("service_center", saved.id, "create", currentUserService.currentUserId())
        return saved.toResponse()
    }

    @Transactional
    fun update(id: Long, request: UpdateServiceCenterRequest): ServiceCenterAdminResponse {
        val center = serviceCenterRepository.findById(id).orElseThrow { NotFoundException("Service center not found") }
        request.name?.takeIf { it.isNotBlank() }?.let { center.name = it }
        request.country?.let { center.country = it }
        request.city?.let { center.city = it }
        request.address?.let { center.address = it }
        request.active?.let { active ->
            if (!active && technicianRepository.countByServiceCenter_IdAndUser_ActiveTrue(id) > 0) {
                throw ConflictException("Cannot deactivate: active technicians are still assigned to this center")
            }
            center.isActive = active
        }
        serviceCenterRepository.save(center)
        auditService.record("service_center", id, "update", currentUserService.currentUserId())
        return center.toResponse()
    }

    private fun ServiceCenter.toResponse() = ServiceCenterAdminResponse(
        id = id,
        code = code,
        name = name,
        country = country,
        city = city,
        address = address,
        active = isActive,
        technicianCount = technicianRepository.countByServiceCenter_Id(id),
    )
}
