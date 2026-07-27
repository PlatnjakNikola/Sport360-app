package com.sport360.moduleservice.testsupport

import com.sport360.moduleservice.common.ApiResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Test-only endpoint to verify role-based method security (@PreAuthorize) is wired. */
@RestController
@RequestMapping("/api/v1/test")
class AdminOnlyTestController {

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    fun adminOnly(): ApiResponse<String> = ApiResponse.ok("ok")
}
