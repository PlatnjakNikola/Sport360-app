package com.sport360.moduleservice.packages.repository

import com.sport360.moduleservice.packages.domain.Package
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PackageRepository : JpaRepository<Package, Long> {

    fun findByIdAndClientIdAndDeletedAtIsNull(id: Long, clientId: Long): Package?

    fun findByIdAndServiceCenterIdAndDeletedAtIsNull(id: Long, serviceCenterId: Long): Package?

    fun findByIdAndDeletedAtIsNull(id: Long): Package?

    fun existsByPackageNumberAndDeletedAtIsNull(packageNumber: String): Boolean

    @Query(
        """
        select p from Package p
        where p.clientId = :clientId and p.deletedAt is null
          and (:statusCode is null or p.currentStatus.code = :statusCode)
          and (:search is null or lower(p.packageNumber) like lower(concat('%', cast(:search as string), '%')))
        order by p.createdAt desc
        """,
    )
    fun findClientPackages(
        @Param("clientId") clientId: Long,
        @Param("statusCode") statusCode: String?,
        @Param("search") search: String?,
        pageable: Pageable,
    ): Page<Package>

    @Query(
        """
        select p.currentStatus.code as code, p.currentStatus.name as name, count(p) as count
        from Package p
        where p.clientId = :clientId and p.deletedAt is null
        group by p.currentStatus.code, p.currentStatus.name
        """,
    )
    fun countByStatusForClient(@Param("clientId") clientId: Long): List<StatusCountProjection>
}
