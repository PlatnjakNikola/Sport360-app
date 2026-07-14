package com.sport360.moduleservice.admin.service

import com.sport360.moduleservice.admin.web.CreateProblemTypeRequest
import com.sport360.moduleservice.admin.web.ProblemTypeAdminResponse
import com.sport360.moduleservice.admin.web.UpdateProblemTypeRequest
import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.modules.repository.ModuleRepository
import com.sport360.moduleservice.problemtypes.domain.ProblemType
import com.sport360.moduleservice.problemtypes.repository.ProblemTypeRepository
import com.sport360.moduleservice.security.CurrentUserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Admin CRUD for problem types. Code is immutable; deactivate instead of delete. */
@Service
class ProblemTypeAdminService(
    private val problemTypeRepository: ProblemTypeRepository,
    private val moduleRepository: ModuleRepository,
    private val currentUserService: CurrentUserService,
    private val auditService: AuditService,
) {

    @Transactional(readOnly = true)
    fun list(): List<ProblemTypeAdminResponse> =
        problemTypeRepository.findAllByOrderBySortOrderAsc().map { it.toResponse() }

    @Transactional
    fun create(request: CreateProblemTypeRequest): ProblemTypeAdminResponse {
        if (problemTypeRepository.existsByCode(request.code)) throw ConflictException("Code already in use")
        if (problemTypeRepository.existsBySortOrder(request.sortOrder)) throw ConflictException("Sort order already in use")
        val nextId = (problemTypeRepository.maxId() + 1).toShort()
        val saved = problemTypeRepository.save(ProblemType(nextId, request.code, request.name, request.sortOrder))
        auditService.record("problem_type", saved.id.toLong(), "create", currentUserService.currentUserId())
        return saved.toResponse()
    }

    @Transactional
    fun update(id: Short, request: UpdateProblemTypeRequest): ProblemTypeAdminResponse {
        val type = problemTypeRepository.findById(id).orElseThrow { NotFoundException("Problem type not found") }
        request.name?.takeIf { it.isNotBlank() }?.let { type.name = it }
        request.sortOrder?.let { newOrder ->
            if (problemTypeRepository.existsBySortOrderAndIdNot(newOrder, id)) throw ConflictException("Sort order already in use")
            type.sortOrder = newOrder
        }
        request.active?.let { type.isActive = it }
        problemTypeRepository.save(type)
        auditService.record("problem_type", id.toLong(), "update", currentUserService.currentUserId())
        return type.toResponse()
    }

    private fun ProblemType.toResponse() = ProblemTypeAdminResponse(
        id = id,
        code = code,
        name = name,
        sortOrder = sortOrder,
        active = isActive,
        usageCount = moduleRepository.countByProblemType_Id(id),
    )
}
