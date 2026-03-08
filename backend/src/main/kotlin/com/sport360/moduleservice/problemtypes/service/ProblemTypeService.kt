package com.sport360.moduleservice.problemtypes.service

import com.sport360.moduleservice.problemtypes.repository.ProblemTypeRepository
import com.sport360.moduleservice.problemtypes.web.ProblemTypeResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProblemTypeService(private val problemTypeRepository: ProblemTypeRepository) {

    @Transactional(readOnly = true)
    fun listActive(): List<ProblemTypeResponse> =
        problemTypeRepository.findAllByActiveTrueOrderBySortOrderAsc().map { ProblemTypeResponse(it.id, it.code, it.name) }
}
