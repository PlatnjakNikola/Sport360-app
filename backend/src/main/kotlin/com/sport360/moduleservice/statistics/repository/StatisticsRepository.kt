package com.sport360.moduleservice.statistics.repository

import com.sport360.moduleservice.packages.domain.Package
import com.sport360.moduleservice.packages.repository.StatusCountProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface LabelCountProjection {
    val label: String
    val count: Long
}

interface PeriodCountProjection {
    val period: String
    val count: Long
}

interface PixelChipSumProjection {
    val totalPixels: Long
    val totalChips: Long
}

/**
 * Reporting queries. Anchored on Package, but the JPQL freely spans Module / ModuleRepair / Client.
 * `filter` is 'all' | 'internal' | 'external'; date bounds are always supplied (never null) by the service.
 */
interface StatisticsRepository : JpaRepository<Package, Long> {

    @Query(
        """
        select p.currentStatus.code as code, p.currentStatus.name as name, count(p) as count
        from Package p
        where p.deletedAt is null
          and (:filter = 'all' or (:filter = 'internal' and p.internal = true) or (:filter = 'external' and p.internal = false))
          and p.createdAt >= :from and p.createdAt <= :to
        group by p.currentStatus.code, p.currentStatus.name, p.currentStatus.sortOrder
        order by p.currentStatus.sortOrder
        """,
    )
    fun packagesByStatus(@Param("filter") filter: String, @Param("from") from: OffsetDateTime, @Param("to") to: OffsetDateTime): List<StatusCountProjection>

    @Query(
        """
        select c.companyName as label, count(p) as count
        from Package p, com.sport360.moduleservice.clients.domain.Client c
        where c.userId = p.clientId and p.deletedAt is null
          and (:filter = 'all' or (:filter = 'internal' and p.internal = true) or (:filter = 'external' and p.internal = false))
          and p.createdAt >= :from and p.createdAt <= :to
        group by c.companyName
        order by count(p) desc
        """,
    )
    fun packagesByClient(@Param("filter") filter: String, @Param("from") from: OffsetDateTime, @Param("to") to: OffsetDateTime): List<LabelCountProjection>

    @Query(
        value = """
        select to_char(date_trunc('month', p.created_at), 'YYYY-MM') as period, count(*) as count
        from packages p
        where p.deleted_at is null
          and (:filter = 'all' or (:filter = 'internal' and p.is_internal = true) or (:filter = 'external' and p.is_internal = false))
          and p.created_at >= :from and p.created_at <= :to
        group by date_trunc('month', p.created_at)
        order by date_trunc('month', p.created_at)
        """,
        nativeQuery = true,
    )
    fun packagesByMonth(@Param("filter") filter: String, @Param("from") from: OffsetDateTime, @Param("to") to: OffsetDateTime): List<PeriodCountProjection>

    @Query(
        """
        select m.currentStatus.code as code, m.currentStatus.name as name, count(m) as count
        from Module m, Package p
        where p.id = m.packageId and m.deletedAt is null and p.deletedAt is null
          and (:filter = 'all' or (:filter = 'internal' and p.internal = true) or (:filter = 'external' and p.internal = false))
          and p.createdAt >= :from and p.createdAt <= :to
        group by m.currentStatus.code, m.currentStatus.name, m.currentStatus.sortOrder
        order by m.currentStatus.sortOrder
        """,
    )
    fun moduleStatusCounts(@Param("filter") filter: String, @Param("from") from: OffsetDateTime, @Param("to") to: OffsetDateTime): List<StatusCountProjection>

    @Query(
        """
        select m.problemType.name as label, count(m) as count
        from Module m, Package p
        where p.id = m.packageId and m.deletedAt is null and p.deletedAt is null
          and (:filter = 'all' or (:filter = 'internal' and p.internal = true) or (:filter = 'external' and p.internal = false))
          and p.createdAt >= :from and p.createdAt <= :to
        group by m.problemType.name
        order by count(m) desc
        """,
    )
    fun modulesByProblemType(@Param("filter") filter: String, @Param("from") from: OffsetDateTime, @Param("to") to: OffsetDateTime): List<LabelCountProjection>

    @Query(
        """
        select m.problemType.name as label, count(m) as count
        from Module m
        where m.packageId = :packageId and m.deletedAt is null
        group by m.problemType.name
        order by count(m) desc
        """,
    )
    fun modulesByProblemTypeForPackage(@Param("packageId") packageId: Long): List<LabelCountProjection>

    @Query(
        """
        select coalesce(sum(r.pixelsRepaired), 0) as totalPixels, coalesce(sum(r.chipsReplaced), 0) as totalChips
        from ModuleRepair r, Module m
        where m.id = r.moduleId and m.packageId = :packageId and m.deletedAt is null
        """,
    )
    fun pixelChipSumsForPackage(@Param("packageId") packageId: Long): PixelChipSumProjection
}
