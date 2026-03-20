package com.sport360.moduleservice.modules.repository

import com.sport360.moduleservice.modules.domain.ModuleRepair
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.OffsetDateTime

interface ModuleRepairRepository : JpaRepository<ModuleRepair, Long> {

    fun countByTechnicianIdAndCompletedAtAfter(technicianId: Long, after: OffsetDateTime): Long

    fun countByTechnicianId(technicianId: Long): Long

    @Query("select count(r) from ModuleRepair r where r.technicianId = :techId and r.decisionStatus.code = 'repaired'")
    fun countRepairedByTechnician(@Param("techId") technicianId: Long): Long

    @Query("select coalesce(sum(r.price), 0) from ModuleRepair r where r.technicianId = :techId and r.completedAt > :after")
    fun sumValueByTechnicianSince(@Param("techId") technicianId: Long, @Param("after") after: OffsetDateTime): BigDecimal
}
