package com.sport360.moduleservice.notifications.service

import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.notifications.domain.Notification
import com.sport360.moduleservice.notifications.repository.NotificationRepository
import com.sport360.moduleservice.notifications.web.NotificationResponse
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
import com.sport360.moduleservice.users.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes in-app notifications (called from within other services' transactions) and serves the
 * current user's notification list / unread count / read state.
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val technicianRepository: TechnicianRepository,
) {

    fun notify(userId: Long, type: String, title: String, message: String?, entityType: String?, entityId: Long?) {
        notificationRepository.save(Notification(userId, type, title, message, entityType, entityId))
    }

    /** Sends a notification to the single admin account, if one exists. */
    fun notifyAdmin(type: String, title: String, message: String?, entityType: String?, entityId: Long?) {
        userRepository.findFirstByRole_CodeOrderByIdAsc(ADMIN_ROLE)?.let {
            notify(it.id, type, title, message, entityType, entityId)
        }
    }

    /** Notifies every active technician of a service center, optionally skipping the one who triggered the change. */
    fun notifyServiceCenterTechnicians(
        serviceCenterId: Long,
        type: String,
        title: String,
        message: String?,
        entityType: String?,
        entityId: Long?,
        exceptUserId: Long? = null,
    ) {
        technicianRepository.findAllByServiceCenter_IdAndUser_ActiveTrue(serviceCenterId)
            .filter { it.userId != exceptUserId }
            .forEach { notify(it.userId, type, title, message, entityType, entityId) }
    }

    @Transactional(readOnly = true)
    fun list(userId: Long, unreadOnly: Boolean, pageable: Pageable): PageResponse<NotificationResponse> {
        val page = if (unreadOnly) {
            notificationRepository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
        } else {
            notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
        }
        return PageResponse.from(page) {
            NotificationResponse(it.id, it.type, it.title, it.message, it.entityType, it.entityId, it.isRead, it.createdAt)
        }
    }

    @Transactional(readOnly = true)
    fun unreadCount(userId: Long): Long = notificationRepository.countByUserIdAndReadFalse(userId)

    @Transactional
    fun markRead(userId: Long, id: Long) {
        val notification = notificationRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Notification not found")
        if (!notification.isRead) {
            notification.isRead = true
            notificationRepository.save(notification)
        }
    }

    @Transactional
    fun markAllRead(userId: Long) {
        notificationRepository.markAllRead(userId)
    }

    private companion object {
        const val ADMIN_ROLE = "admin"
    }
}
