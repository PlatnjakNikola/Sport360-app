package com.sport360.moduleservice.clients.repository

import com.sport360.moduleservice.clients.domain.Client
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ClientRepository : JpaRepository<Client, Long> {
    fun findAllByOrderByUserIdAsc(pageable: Pageable): Page<Client>
}
