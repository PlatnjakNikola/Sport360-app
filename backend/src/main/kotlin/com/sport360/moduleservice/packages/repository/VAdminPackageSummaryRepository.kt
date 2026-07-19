package com.sport360.moduleservice.packages.repository

import com.sport360.moduleservice.packages.domain.VAdminPackageSummary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface VAdminPackageSummaryRepository : JpaRepository<VAdminPackageSummary, Long> {

    @Query(
        """
        select v from VAdminPackageSummary v
        where (:includeDeleted = true or v.deletedAt is null)
          and (:statusCode is null or v.statusCode = :statusCode)
          and (:clientId is null or v.clientId = :clientId)
          and (:serviceCenterId is null or v.serviceCenterId = :serviceCenterId)
          and (:type is null
               or (:type = 'internal' and v.internal = true)
               or (:type = 'external' and v.internal = false))
          and (:search is null
               or lower(v.packageNumber) like lower(concat('%', cast(:search as string), '%'))
               or lower(v.companyName) like lower(concat('%', cast(:search as string), '%')))
          and v.createdAt >= :dateFrom
          and v.createdAt <= :dateTo
        order by v.createdAt desc
        """,
    )
    fun search(
        @Param("includeDeleted") includeDeleted: Boolean,
        @Param("statusCode") statusCode: String?,
        @Param("clientId") clientId: Long?,
        @Param("serviceCenterId") serviceCenterId: Long?,
        @Param("type") type: String?,
        @Param("search") search: String?,
        @Param("dateFrom") dateFrom: OffsetDateTime?,
        @Param("dateTo") dateTo: OffsetDateTime?,
        pageable: Pageable,
    ): Page<VAdminPackageSummary>

    fun findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(pageable: Pageable): Page<VAdminPackageSummary>
}
