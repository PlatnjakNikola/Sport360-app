package com.sport360.moduleservice.users.repository

import com.sport360.moduleservice.users.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmailIgnoreCase(email: String): User?
    fun existsByEmailIgnoreCase(email: String): Boolean
    fun existsByRole_Code(roleCode: String): Boolean
    fun findFirstByRole_CodeOrderByIdAsc(roleCode: String): User?
}
