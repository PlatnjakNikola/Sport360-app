package com.sport360.moduleservice.modules.repository

import com.sport360.moduleservice.modules.domain.ModuleStatusHistory
import org.springframework.data.jpa.repository.JpaRepository

interface ModuleStatusHistoryRepository : JpaRepository<ModuleStatusHistory, Long>
