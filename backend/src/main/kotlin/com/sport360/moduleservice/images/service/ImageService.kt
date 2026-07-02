package com.sport360.moduleservice.images.service

import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.ForbiddenException
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.ValidationException
import com.sport360.moduleservice.config.AppProperties
import com.sport360.moduleservice.images.storage.ImageStorageService
import com.sport360.moduleservice.modules.domain.ModuleImage
import com.sport360.moduleservice.modules.repository.ModuleImageRepository
import com.sport360.moduleservice.modules.repository.ModuleRepository
import com.sport360.moduleservice.modules.web.ModuleImageResponse
import com.sport360.moduleservice.packages.domain.Package
import com.sport360.moduleservice.packages.domain.PackageStatus
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.packages.repository.PackageStatusRepository
import com.sport360.moduleservice.security.CurrentUserService
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
import com.sport360.moduleservice.technicianworkflow.service.TechnicianContext
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

/** A served image: its bytes and the content type derived from the stored extension. */
data class ServedImage(val resource: Resource, val contentType: String)

/** Module image upload, access-checked serving, list/thumbnail helpers, and the 30-day cleanup. */
@Service
class ImageService(
    private val moduleImageRepository: ModuleImageRepository,
    private val moduleRepository: ModuleRepository,
    private val packageRepository: PackageRepository,
    private val packageStatusRepository: PackageStatusRepository,
    private val technicianRepository: TechnicianRepository,
    private val technicianContext: TechnicianContext,
    private val currentUserService: CurrentUserService,
    private val storage: ImageStorageService,
    private val props: AppProperties,
) {

    // ---- Upload (technician) ----

    @Transactional
    fun uploadForModule(moduleId: Long, files: List<MultipartFile>): List<ModuleImageResponse> {
        val technician = technicianContext.current()
        val module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId) ?: throw NotFoundException("Module not found")
        packageRepository.findByIdAndServiceCenterIdAndDeletedAtIsNull(module.packageId, technician.serviceCenter.id)
            ?: throw NotFoundException("Module not found")

        if (files.isEmpty() || files.all { it.isEmpty }) throw ValidationException("No files provided")
        if (moduleImageRepository.countByModuleId(moduleId) + files.size > MAX_PER_MODULE) {
            throw ConflictException("A module can have at most $MAX_PER_MODULE images")
        }
        // Validate every file up front so a bad one in the batch fails before anything is stored.
        files.forEach { file ->
            if (file.size > MAX_SIZE_BYTES) throw ValidationException("Each image must be 20MB or smaller")
            if (file.contentType !in ALLOWED_TYPES) {
                throw ValidationException("Unsupported image type — only JPEG, PNG and WebP are allowed")
            }
        }
        files.forEach { file ->
            val extension = ALLOWED_TYPES.getValue(file.contentType!!)
            val key = storage.save(moduleId, file.bytes, extension)
            moduleImageRepository.save(ModuleImage(moduleId, key, technician.userId))
        }
        return imagesForModule(moduleId)
    }

    // ---- Serving (access-checked) ----

    @Transactional(readOnly = true)
    fun serve(imageId: Long): ServedImage {
        val image = moduleImageRepository.findById(imageId).orElseThrow { NotFoundException("Image not found") }
        val module = moduleRepository.findByIdAndDeletedAtIsNull(image.moduleId)
            ?: throw NotFoundException("Image not found")
        val pkg = packageRepository.findById(module.packageId)
            .filter { it.deletedAt == null }
            .orElseThrow { NotFoundException("Image not found") }
        assertCanView(pkg)
        return ServedImage(storage.load(image.filePath), contentTypeFor(image.filePath))
    }

    /** Per-role access: admin any; technician own center; client own package once unlocked. */
    private fun assertCanView(pkg: Package) {
        val principal = currentUserService.currentPrincipal()
        when (principal.role) {
            "admin" -> Unit
            "technician" -> {
                val technician = technicianRepository.findById(principal.userId)
                    .orElseThrow { ForbiddenException("Not a technician") }
                if (pkg.serviceCenterId != technician.serviceCenter.id) throw NotFoundException("Image not found")
            }
            "client" -> {
                if (pkg.clientId != principal.userId) throw NotFoundException("Image not found")
                requireUnlocked(pkg.currentStatus)
            }
            else -> throw ForbiddenException()
        }
    }

    // ---- Helpers shared with module responses ----

    @Transactional(readOnly = true)
    fun imagesForModule(moduleId: Long): List<ModuleImageResponse> =
        moduleImageRepository.findAllByModuleIdOrderByIdAsc(moduleId).map { ModuleImageResponse(it.id, urlFor(it.id)) }

    /** First-image thumbnail URL per module id, in one query (used by list views). */
    @Transactional(readOnly = true)
    fun thumbnailUrls(moduleIds: Collection<Long>): Map<Long, String> {
        if (moduleIds.isEmpty()) return emptyMap()
        return moduleImageRepository.findFirstImageIds(moduleIds).associate { it.moduleId to urlFor(it.imageId) }
    }

    // ---- Cleanup ----

    /** Hard-deletes images (file + row) whose package arrived more than the retention window ago. */
    @Transactional
    fun cleanupExpired(): Int {
        val cutoff = OffsetDateTime.now().minusDays(props.image.retentionDays)
        val expired = moduleImageRepository.findExpired(cutoff)
        if (expired.isEmpty()) return 0
        expired.forEach { runCatching { storage.delete(it.filePath) } }
        moduleImageRepository.deleteAllInBatch(expired)
        return expired.size
    }

    private fun requireUnlocked(status: PackageStatus) {
        val threshold = packageStatusRepository.findByCode(STATUS_REPAIRED_WAITING)?.sortOrder
            ?: error("status missing")
        if (status.sortOrder < threshold) throw ForbiddenException("Module images are not available yet")
    }

    private fun urlFor(imageId: Long) = "/api/v1/images/$imageId"

    private fun contentTypeFor(key: String): String =
        when (key.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

    private companion object {
        const val MAX_PER_MODULE = 5
        const val MAX_SIZE_BYTES = 20L * 1024 * 1024
        const val STATUS_REPAIRED_WAITING = "repaired_waiting_shipment"
        val ALLOWED_TYPES = mapOf(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
        )
    }
}
