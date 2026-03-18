package com.sport360.moduleservice.packages.repository

import com.sport360.moduleservice.packages.domain.VPackageSummary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VPackageSummaryRepository : JpaRepository<VPackageSummary, Long> {

    @Query(
        """
        select v from VPackageSummary v
        where v.serviceCenterId = :scId and v.statusCode in :statuses
          and (:search is null or lower(v.packageNumber) like lower(concat('%', cast(:search as string), '%')))
        order by v.createdAt desc
        """,
    )
    fun findForServiceCenter(
        @Param("scId") serviceCenterId: Long,
        @Param("statuses") statuses: Collection<String>,
        @Param("search") search: String?,
        pageable: Pageable,
    ): Page<VPackageSummary>

    @Query(
        """
        select v.statusCode as code, v.statusName as name, count(v) as count
        from VPackageSummary v
        where v.serviceCenterId = :scId and v.statusCode in :statuses
        group by v.statusCode, v.statusName
        """,
    )
    fun countByStatusForServiceCenter(
        @Param("scId") serviceCenterId: Long,
        @Param("statuses") statuses: Collection<String>,
    ): List<StatusCountProjection>

    @Query(
        """
        select v from VPackageSummary v
        where v.serviceCenterId = :scId and v.internal = true
          and (:statusCode is null or v.statusCode = :statusCode)
          and (:search is null or lower(v.packageNumber) like lower(concat('%', cast(:search as string), '%')))
        order by v.createdAt desc
        """,
    )
    fun findInternalForServiceCenter(
        @Param("scId") serviceCenterId: Long,
        @Param("statusCode") statusCode: String?,
        @Param("search") search: String?,
        pageable: Pageable,
    ): Page<VPackageSummary>
}
