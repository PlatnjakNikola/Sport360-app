package com.sport360.moduleservice.statistics.web

import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.statistics.service.StatisticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class StatisticsController(private val statisticsService: StatisticsService) {

    @GetMapping("/dashboard")
    fun dashboard(): ApiResponse<DashboardResponse> = ApiResponse.ok(statisticsService.dashboard())

    @GetMapping("/statistics")
    fun statistics(
        @RequestParam(required = false) filter: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateTo: OffsetDateTime?,
    ): ApiResponse<GlobalStatisticsResponse> = ApiResponse.ok(statisticsService.global(filter, dateFrom, dateTo))

    @GetMapping("/statistics/export.csv")
    fun exportCsv(
        @RequestParam(required = false) filter: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) dateTo: OffsetDateTime?,
    ): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statistics.csv\"")
            .body(statisticsService.globalCsv(filter, dateFrom, dateTo))

    @GetMapping("/packages/{id}/statistics")
    fun packageStatistics(@PathVariable id: Long): ApiResponse<PackageStatisticsResponse> =
        ApiResponse.ok(statisticsService.forPackage(id))
}
