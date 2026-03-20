package com.sport360.moduleservice.users.repository

import com.sport360.moduleservice.users.domain.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Short> {
    fun findByCode(code: String): Role?
}
