package com.sport360.moduleservice.technicianworkflow.service

import com.sport360.moduleservice.common.ForbiddenException
import com.sport360.moduleservice.security.CurrentUserService
import com.sport360.moduleservice.technicians.domain.Technician
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
import org.springframework.stereotype.Service

/** Resolves the current technician + their service center for scoping queries. */
@Service
class TechnicianContext(
    private val technicianRepository: TechnicianRepository,
    private val currentUserService: CurrentUserService,
) {
    fun current(): Technician =
        technicianRepository.findById(currentUserService.currentUserId())
            .orElseThrow { ForbiddenException("Not a technician") }

    fun serviceCenterId(): Long = current().serviceCenter.id
}
