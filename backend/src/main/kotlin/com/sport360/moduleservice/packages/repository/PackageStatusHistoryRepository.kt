package com.sport360.moduleservice.packages.repository

import com.sport360.moduleservice.packages.domain.PackageStatusHistory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PackageStatusHistoryRepository : JpaRepository<PackageStatusHistory, Long> {

    @Query(
        """
        select ps.code as statusCode, ps.name as statusName, h.changedAt as changedAt
        from PackageStatusHistory h, PackageStatus ps
        where ps.id = h.statusId and h.packageId = :packageId
        order by h.changedAt asc
        """,
    )
    fun timeline(@Param("packageId") packageId: Long): List<TimelineProjection>

    @Query(
        """
        select p.id as packageId, p.packageNumber as packageNumber,
               ps.code as statusCode, ps.name as statusName, h.changedAt as changedAt
        from PackageStatusHistory h, Package p, PackageStatus ps
        where p.id = h.packageId and ps.id = h.statusId
          and p.clientId = :clientId and p.deletedAt is null
        order by h.changedAt desc
        """,
    )
    fun recentActivityForClient(@Param("clientId") clientId: Long, pageable: Pageable): List<RecentActivityProjection>

    @Query(
        """
        select p.id as packageId, p.packageNumber as packageNumber,
               ps.code as statusCode, ps.name as statusName, h.changedAt as changedAt
        from PackageStatusHistory h, Package p, PackageStatus ps
        where p.id = h.packageId and ps.id = h.statusId
          and p.serviceCenterId = :serviceCenterId and p.deletedAt is null
        order by h.changedAt desc
        """,
    )
    fun recentActivityForServiceCenter(
        @Param("serviceCenterId") serviceCenterId: Long,
        pageable: Pageable,
    ): List<RecentActivityProjection>

    @Query(
        """
        select p.id as packageId, p.packageNumber as packageNumber,
               ps.code as statusCode, ps.name as statusName, h.changedAt as changedAt
        from PackageStatusHistory h, Package p, PackageStatus ps
        where p.id = h.packageId and ps.id = h.statusId and p.deletedAt is null
        order by h.changedAt desc
        """,
    )
    fun recentActivity(pageable: Pageable): List<RecentActivityProjection>
}
