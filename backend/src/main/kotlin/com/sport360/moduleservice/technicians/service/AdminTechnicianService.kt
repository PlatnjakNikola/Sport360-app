package com.sport360.moduleservice.technicians.service

import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.ValidationException
import com.sport360.moduleservice.servicecenters.repository.ServiceCenterRepository
import com.sport360.moduleservice.technicians.domain.Technician
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
import com.sport360.moduleservice.technicians.web.TechnicianResponse
import com.sport360.moduleservice.technicians.web.UpdateTechnicianRequest
import com.sport360.moduleservice.users.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminTechnicianService(
    private val technicianRepository: TechnicianRepository,
    private val userRepository: UserRepository,
    private val serviceCenterRepository: ServiceCenterRepository,
    private val auditService: AuditService,
) {

    @Transactional(readOnly = true)
    fun list(pageable: Pageable): PageResponse<TechnicianResponse> =
        PageResponse.from(technicianRepository.findAllByOrderByUserIdAsc(pageable)) { it.toResponse() }

    @Transactional(readOnly = true)
    fun get(id: Long): TechnicianResponse =
        technicianRepository.findById(id).orElseThrow { NotFoundException("Technician not found") }.toResponse()

    @Transactional
    fun update(adminId: Long, id: Long, request: UpdateTechnicianRequest): TechnicianResponse {
        val technician = technicianRepository.findById(id)
            .orElseThrow { NotFoundException("Technician not found") }
        request.name?.let { technician.user.name = it }
        request.phone?.let { technician.phone = it }
        request.serviceCenterId?.let { serviceCenterId ->
            technician.serviceCenter = serviceCenterRepository.findByIdAndActiveTrue(serviceCenterId)
                ?: throw ValidationException("Service center not found or inactive")
        }
        request.isActive?.let { technician.user.isActive = it }
        userRepository.save(technician.user)
        technicianRepository.save(technician)
        auditService.record("user", technician.userId, "update", adminId)
        return technician.toResponse()
    }

    private fun Technician.toResponse() = TechnicianResponse(
        userId = userId,
        name = user.name,
        email = user.email,
        phone = phone,
        serviceCenterId = serviceCenter.id,
        serviceCenterName = serviceCenter.name,
        active = user.isActive,
        createdAt = user.createdAt,
    )
}
