package com.sport360.moduleservice.statistics.service

import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.invites.service.InviteService
import com.sport360.moduleservice.packages.repository.PackageStatusHistoryRepository
import com.sport360.moduleservice.packages.repository.VAdminPackageSummaryRepository
import com.sport360.moduleservice.packages.repository.VPackageTechnicianBreakdownRepository
import com.sport360.moduleservice.packages.web.ActivityItem
import com.sport360.moduleservice.statistics.repository.StatisticsRepository
import com.sport360.moduleservice.statistics.web.CountItem
import com.sport360.moduleservice.statistics.web.DashboardResponse
import com.sport360.moduleservice.statistics.web.GlobalStatisticsResponse
import com.sport360.moduleservice.statistics.web.PackageStatisticsResponse
import com.sport360.moduleservice.statistics.web.PeriodCountItem
import com.sport360.moduleservice.statistics.web.StatusCountItem
import com.sport360.moduleservice.statistics.web.TechnicianBreakdownItem
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Global + per-package statistics, the admin dashboard, and CSV export of the global report. */
@Service
class StatisticsService(
    private val statisticsRepository: StatisticsRepository,
    private val adminSummaryRepository: VAdminPackageSummaryRepository,
    private val technicianBreakdownRepository: VPackageTechnicianBreakdownRepository,
    private val packageStatusHistoryRepository: PackageStatusHistoryRepository,
    private val inviteService: InviteService,
) {

    @Transactional(readOnly = true)
    fun global(filter: String?, from: OffsetDateTime?, to: OffsetDateTime?): GlobalStatisticsResponse {
        val f = normalizeFilter(filter)
        val start = from ?: DATE_FLOOR
        val end = to ?: DATE_CEILING

        val byStatus = statisticsRepository.packagesByStatus(f, start, end).map { StatusCountItem(it.code, it.name, it.count) }
        val moduleStatus = statisticsRepository.moduleStatusCounts(f, start, end)
        return GlobalStatisticsResponse(
            totalPackages = byStatus.sumOf { it.count },
            packagesByStatus = byStatus,
            packagesByClient = statisticsRepository.packagesByClient(f, start, end).map { CountItem(it.label, it.count) },
            packagesByMonth = statisticsRepository.packagesByMonth(f, start, end).map { PeriodCountItem(it.period, it.count) },
            totalModules = moduleStatus.sumOf { it.count },
            repairedModules = moduleStatus.firstOrNull { it.code == "repaired" }?.count ?: 0,
            notRepairableModules = moduleStatus.firstOrNull { it.code == "not_repairable" }?.count ?: 0,
            modulesByProblemType = statisticsRepository.modulesByProblemType(f, start, end).map { CountItem(it.label, it.count) },
        )
    }

    @Transactional(readOnly = true)
    fun forPackage(packageId: Long): PackageStatisticsResponse {
        val summary = adminSummaryRepository.findById(packageId).orElseThrow { NotFoundException("Package not found") }
        val sums = statisticsRepository.pixelChipSumsForPackage(packageId)
        return PackageStatisticsResponse(
            packageId = packageId,
            totalModules = summary.totalModules,
            repairedCount = summary.repairedCount,
            notRepairableCount = summary.notRepairableCount,
            totalPixels = sums.totalPixels,
            totalChips = sums.totalChips,
            totalValue = summary.totalValue,
            modulesByProblemType = statisticsRepository.modulesByProblemTypeForPackage(packageId).map { CountItem(it.label, it.count) },
            technicianBreakdown = technicianBreakdownRepository.findAllByPackageIdOrderByTechnicianNameAsc(packageId).map {
                TechnicianBreakdownItem(
                    technicianId = it.technicianId,
                    technicianName = it.technicianName,
                    moduleCount = it.moduleCount,
                    repairedCount = it.repairedCount,
                    notRepairableCount = it.notRepairableCount,
                    totalPixels = it.totalPixels,
                    totalChips = it.totalChips,
                    totalValue = it.totalValue,
                )
            },
        )
    }

    @Transactional(readOnly = true)
    fun dashboard(): DashboardResponse {
        val byStatus = statisticsRepository.packagesByStatus("all", DATE_FLOOR, DATE_CEILING).map { StatusCountItem(it.code, it.name, it.count) }
        val moduleStatus = statisticsRepository.moduleStatusCounts("all", DATE_FLOOR, DATE_CEILING)
        val total = byStatus.sumOf { it.count }
        val completed = byStatus.firstOrNull { it.code == "arrived" }?.count ?: 0
        val activity = packageStatusHistoryRepository.recentActivity(PageRequest.of(0, 10))
            .map { ActivityItem(it.packageId, it.packageNumber, it.statusCode, it.statusName, it.changedAt) }
        return DashboardResponse(
            totalPackages = total,
            activePackages = total - completed,
            completedPackages = completed,
            packagesByStatus = byStatus,
            totalModules = moduleStatus.sumOf { it.count },
            repairedModules = moduleStatus.firstOrNull { it.code == "repaired" }?.count ?: 0,
            notRepairableModules = moduleStatus.firstOrNull { it.code == "not_repairable" }?.count ?: 0,
            pendingTechnicianInvites = inviteService.listPendingTechnicianInvites().size,
            pendingClientInvites = inviteService.listPendingClientInvites().size,
            recentActivity = activity,
        )
    }

    /** Renders the global report as a multi-section CSV. */
    @Transactional(readOnly = true)
    fun globalCsv(filter: String?, from: OffsetDateTime?, to: OffsetDateTime?): String {
        val stats = global(filter, from, to)
        val sb = StringBuilder()
        sb.appendLine("Packages by status")
        sb.appendLine("Status,Count")
        stats.packagesByStatus.forEach { sb.appendLine("${csv(it.name)},${it.count}") }
        sb.appendLine()
        sb.appendLine("Packages by client")
        sb.appendLine("Client,Count")
        stats.packagesByClient.forEach { sb.appendLine("${csv(it.label)},${it.count}") }
        sb.appendLine()
        sb.appendLine("Packages by month")
        sb.appendLine("Month,Count")
        stats.packagesByMonth.forEach { sb.appendLine("${csv(it.period)},${it.count}") }
        sb.appendLine()
        sb.appendLine("Modules")
        sb.appendLine("Metric,Count")
        sb.appendLine("Total,${stats.totalModules}")
        sb.appendLine("Repaired,${stats.repairedModules}")
        sb.appendLine("Not repairable,${stats.notRepairableModules}")
        sb.appendLine()
        sb.appendLine("Modules by problem type")
        sb.appendLine("Problem type,Count")
        stats.modulesByProblemType.forEach { sb.appendLine("${csv(it.label)},${it.count}") }
        return sb.toString()
    }

    private fun normalizeFilter(filter: String?): String =
        when (filter?.lowercase()) {
            "internal" -> "internal"
            "external" -> "external"
            else -> "all"
        }

    /** Quotes a CSV field if it contains a comma, quote or newline. */
    private fun csv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value

    private companion object {
        val DATE_FLOOR: OffsetDateTime = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val DATE_CEILING: OffsetDateTime = OffsetDateTime.of(2200, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    }
}
