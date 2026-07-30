package com.sport360.moduleservice.cleanup

import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.images.service.ImageService
import com.sport360.moduleservice.users.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Daily cleanup of module images whose package arrived more than the retention window ago.
 * Removes both the stored file and the DB row, and records a single audit batch entry.
 */
@Component
class ImageCleanupJob(
    private val imageService: ImageService,
    private val auditService: AuditService,
    private val userRepository: UserRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.image.cleanup-cron:0 0 3 * * *}")
    fun purgeExpiredImages() {
        val deleted = imageService.cleanupExpired()
        if (deleted == 0) return
        log.info("Image cleanup removed {} expired module image(s)", deleted)
        userRepository.findFirstByRole_CodeOrderByIdAsc(ADMIN_ROLE)?.let { admin ->
            auditService.record(
                entityType = "image",
                entityId = 0,
                actionType = "cleanup",
                changedByUserId = admin.id,
                newValueJson = """{"deletedImages":$deleted}""",
            )
        }
    }

    private companion object {
        const val ADMIN_ROLE = "admin"
    }
}
