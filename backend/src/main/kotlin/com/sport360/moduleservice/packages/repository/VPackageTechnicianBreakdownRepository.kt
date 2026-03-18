package com.sport360.moduleservice.packages.repository

import com.sport360.moduleservice.packages.domain.VPackageTechnicianBreakdown
import org.springframework.data.jpa.repository.JpaRepository

interface VPackageTechnicianBreakdownRepository : JpaRepository<VPackageTechnicianBreakdown, Long> {
    fun findAllByPackageIdOrderByTechnicianNameAsc(packageId: Long): List<VPackageTechnicianBreakdown>
}
