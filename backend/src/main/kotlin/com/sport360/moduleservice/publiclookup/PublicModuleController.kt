package com.sport360.moduleservice.publiclookup

import com.sport360.moduleservice.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Public, unauthenticated module service-history lookup. Rate limited per IP in RateLimitFilter. */
@RestController
@RequestMapping("/api/v1/public/modules")
class PublicModuleController(private val publicModuleService: PublicModuleService) {

    @GetMapping("/{moduleNumber}/history")
    fun history(@PathVariable moduleNumber: String): ApiResponse<List<PublicModuleVisitResponse>> =
        ApiResponse.ok(publicModuleService.history(moduleNumber))
}
