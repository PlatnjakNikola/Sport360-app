package com.sport360.moduleservice.statistics.web

import com.sport360.moduleservice.packages.web.ActivityItem
import java.math.BigDecimal

data class CountItem(val label: String, val count: Long)

data class StatusCountItem(val code: String, val name: String, val count: Long)

data class PeriodCountItem(val period: String, val count: Long)

data class GlobalStatisticsResponse(
    val totalPackages: Long,
    val packagesByStatus: List<StatusCountItem>,
    val packagesByClient: List<CountItem>,
    val packagesByMonth: List<PeriodCountItem>,
    val totalModules: Long,
    val repairedModules: Long,
    val notRepairableModules: Long,
    val modulesByProblemType: List<CountItem>,
)

data class TechnicianBreakdownItem(
    val technicianId: Long,
    val technicianName: String,
    val moduleCount: Long,
    val repairedCount: Long,
    val notRepairableCount: Long,
    val totalPixels: Long,
    val totalChips: Long,
    val totalValue: BigDecimal,
)

data class PackageStatisticsResponse(
    val packageId: Long,
    val totalModules: Long,
    val repairedCount: Long,
    val notRepairableCount: Long,
    val totalPixels: Long,
    val totalChips: Long,
    val totalValue: BigDecimal,
    val modulesByProblemType: List<CountItem>,
    val technicianBreakdown: List<TechnicianBreakdownItem>,
)

data class DashboardResponse(
    val totalPackages: Long,
    val activePackages: Long,
    val completedPackages: Long,
    val packagesByStatus: List<StatusCountItem>,
    val totalModules: Long,
    val repairedModules: Long,
    val notRepairableModules: Long,
    val pendingTechnicianInvites: Int,
    val pendingClientInvites: Int,
    val recentActivity: List<ActivityItem>,
)
