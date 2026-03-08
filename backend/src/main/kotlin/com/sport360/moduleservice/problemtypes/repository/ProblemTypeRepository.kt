package com.sport360.moduleservice.problemtypes.repository

import com.sport360.moduleservice.problemtypes.domain.ProblemType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProblemTypeRepository : JpaRepository<ProblemType, Short> {
    fun findAllByActiveTrueOrderBySortOrderAsc(): List<ProblemType>
    fun findAllByOrderBySortOrderAsc(): List<ProblemType>
    fun findByIdAndActiveTrue(id: Short): ProblemType?
    fun existsByCode(code: String): Boolean
    fun existsBySortOrder(sortOrder: Short): Boolean
    fun existsBySortOrderAndIdNot(sortOrder: Short, id: Short): Boolean

    @Query("select coalesce(max(p.id), 0) from ProblemType p")
    fun maxId(): Int
}
