package com.sport360.moduleservice.admin.service

import com.sport360.moduleservice.admin.web.TrashItemResponse
import com.sport360.moduleservice.modules.repository.ModuleRepository
import com.sport360.moduleservice.packages.repository.VAdminPackageSummaryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Lists soft-deleted packages and modules for the admin Trash page. Restore lives on the package/module services. */
@Service
class AdminTrashService(
    private val adminSummaryRepository: VAdminPackageSummaryRepository,
    private val moduleRepository: ModuleRepository,
) {

    @Transactional(readOnly = true)
    fun list(): List<TrashItemResponse> {
        val cap = PageRequest.of(0, 200)
        val packages = adminSummaryRepository.findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(cap).content
            .map { TrashItemResponse("package", it.id, it.packageNumber, null, it.deletedAt) }
        val modules = moduleRepository.findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(cap).content
            .map { TrashItemResponse("module", it.id, it.moduleNumber, it.packageId, it.deletedAt) }
        return (packages + modules).sortedByDescending { it.deletedAt }
    }
}
