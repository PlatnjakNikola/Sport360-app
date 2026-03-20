package com.sport360.moduleservice.packages.repository

import com.sport360.moduleservice.packages.domain.PackageStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PackageStatusRepository : JpaRepository<PackageStatus, Short> {
    fun findByCode(code: String): PackageStatus?
    fun findBySortOrder(sortOrder: Short): PackageStatus?
}
