package com.sport360.moduleservice.servicecenters.repository

import com.sport360.moduleservice.servicecenters.domain.ServiceCenter
import org.springframework.data.jpa.repository.JpaRepository

interface ServiceCenterRepository : JpaRepository<ServiceCenter, Long> {
    fun findAllByActiveTrueOrderByNameAsc(): List<ServiceCenter>
    fun findAllByOrderByNameAsc(): List<ServiceCenter>
    fun findByIdAndActiveTrue(id: Long): ServiceCenter?
    fun findByCode(code: String): ServiceCenter?
    fun existsByCode(code: String): Boolean
}
