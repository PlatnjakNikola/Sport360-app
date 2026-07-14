package com.sport360.moduleservice.admin.web

import com.sport360.moduleservice.admin.service.ProblemTypeAdminService
import com.sport360.moduleservice.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/problem-types")
@PreAuthorize("hasRole('ADMIN')")
class AdminProblemTypeController(private val problemTypeAdminService: ProblemTypeAdminService) {

    @GetMapping
    fun list(): ApiResponse<List<ProblemTypeAdminResponse>> = ApiResponse.ok(problemTypeAdminService.list())

    @PostMapping
    fun create(@Valid @RequestBody request: CreateProblemTypeRequest): ApiResponse<ProblemTypeAdminResponse> =
        ApiResponse.ok(problemTypeAdminService.create(request))

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Short, @Valid @RequestBody request: UpdateProblemTypeRequest): ApiResponse<ProblemTypeAdminResponse> =
        ApiResponse.ok(problemTypeAdminService.update(id, request))
}
