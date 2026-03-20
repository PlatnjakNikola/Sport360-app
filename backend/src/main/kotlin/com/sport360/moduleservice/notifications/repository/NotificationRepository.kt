package com.sport360.moduleservice.notifications.repository

import com.sport360.moduleservice.notifications.domain.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>

    fun findAllByUserIdAndReadFalseOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>

    fun countByUserIdAndReadFalse(userId: Long): Long

    fun findByIdAndUserId(id: Long, userId: Long): Notification?

    @Modifying
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    fun markAllRead(@Param("userId") userId: Long): Int
}
