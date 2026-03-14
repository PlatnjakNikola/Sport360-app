package com.sport360.moduleservice.modules.repository

import com.sport360.moduleservice.modules.domain.Module
import com.sport360.moduleservice.publiclookup.PublicModuleHistoryProjection
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ModuleRepository : JpaRepository<Module, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Module?

    fun findAllByPackageIdAndDeletedAtIsNullOrderByCreatedAtAsc(packageId: Long, pageable: Pageable): Page<Module>

    fun findAllByPackageIdAndDeletedAtIsNullOrderByCreatedAtAsc(packageId: Long): List<Module>

    fun findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(pageable: Pageable): Page<Module>

    fun existsByPackageIdAndModuleNumberAndDeletedAtIsNull(packageId: Long, moduleNumber: String): Boolean

    fun findFirstByPackageIdAndModuleNumberAndDeletedAtIsNull(packageId: Long, moduleNumber: String): Module?

    fun countByProblemType_Id(problemTypeId: Short): Long

    @Query("select count(m) from Module m where m.packageId = :packageId and m.deletedAt is null")
    fun countActive(@Param("packageId") packageId: Long): Long

    @Query(
        "select count(m) from Module m " +
            "where m.packageId = :packageId and m.deletedAt is null and m.currentStatus.finalStatus = false",
    )
    fun countUnfinished(@Param("packageId") packageId: Long): Long

    /** Public, privacy-safe service history for a module number (newest visit first). */
    @Query(
        """
        select ms.code as statusCode, ms.name as statusName, pt.name as problemTypeName,
               r.pixelsRepaired as pixelsRepaired, r.chipsReplaced as chipsReplaced,
               r.repairNote as repairNote, r.completedAt as completedAt,
               p.receivedAt as receivedAt, p.serviceCompletedAt as serviceCompletedAt,
               p.shippedAt as shippedAt, p.arrivedAt as arrivedAt
        from Module m
        join m.currentStatus ms
        join m.problemType pt
        join Package p on p.id = m.packageId
        left join ModuleRepair r on r.moduleId = m.id
        where m.moduleNumber = :moduleNumber and m.deletedAt is null and p.deletedAt is null
        order by m.createdAt desc
        """,
    )
    fun findPublicHistory(@Param("moduleNumber") moduleNumber: String): List<PublicModuleHistoryProjection>
}
