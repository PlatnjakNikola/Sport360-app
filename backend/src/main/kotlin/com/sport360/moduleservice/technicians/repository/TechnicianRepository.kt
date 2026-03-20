package com.sport360.moduleservice.technicians.repository

import com.sport360.moduleservice.technicians.domain.Technician
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TechnicianRepository : JpaRepository<Technician, Long> {
    fun findAllByOrderByUserIdAsc(pageable: Pageable): Page<Technician>
    fun findAllByServiceCenter_IdAndUser_ActiveTrue(serviceCenterId: Long): List<Technician>
    fun countByServiceCenter_Id(serviceCenterId: Long): Long
    fun countByServiceCenter_IdAndUser_ActiveTrue(serviceCenterId: Long): Long
}
