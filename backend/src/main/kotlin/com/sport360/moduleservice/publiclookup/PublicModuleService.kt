package com.sport360.moduleservice.publiclookup

import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.modules.repository.ModuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/** A single privacy-safe service visit for the public lookup. No prices, names, numbers or images. */
data class PublicModuleVisitResponse(
    val statusCode: String,
    val statusName: String,
    val problemTypeName: String,
    val pixelsRepaired: Int?,
    val chipsReplaced: Int?,
    val repairNote: String?,
    val completedAt: OffsetDateTime?,
    val receivedAt: OffsetDateTime?,
    val serviceCompletedAt: OffsetDateTime?,
    val shippedAt: OffsetDateTime?,
    val arrivedAt: OffsetDateTime?,
)

@Service
class PublicModuleService(private val moduleRepository: ModuleRepository) {

    @Transactional(readOnly = true)
    fun history(moduleNumber: String): List<PublicModuleVisitResponse> {
        val visits = moduleRepository.findPublicHistory(moduleNumber.trim())
        if (visits.isEmpty()) throw NotFoundException("No service records found for this module number")
        return visits.map {
            PublicModuleVisitResponse(
                statusCode = it.statusCode,
                statusName = it.statusName,
                problemTypeName = it.problemTypeName,
                pixelsRepaired = it.pixelsRepaired?.toInt(),
                chipsReplaced = it.chipsReplaced?.toInt(),
                repairNote = it.repairNote,
                completedAt = it.completedAt,
                receivedAt = it.receivedAt,
                serviceCompletedAt = it.serviceCompletedAt,
                shippedAt = it.shippedAt,
                arrivedAt = it.arrivedAt,
            )
        }
    }
}
