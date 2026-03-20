package com.sport360.moduleservice.modules.repository

import com.sport360.moduleservice.modules.domain.ModuleStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ModuleStatusRepository : JpaRepository<ModuleStatus, Short> {
    fun findByCode(code: String): ModuleStatus?
}
