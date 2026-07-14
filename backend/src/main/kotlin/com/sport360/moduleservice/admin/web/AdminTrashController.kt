package com.sport360.moduleservice.admin.web

import com.sport360.moduleservice.admin.service.AdminTrashService
import com.sport360.moduleservice.common.ApiResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/trash")
@PreAuthorize("hasRole('ADMIN')")
class AdminTrashController(private val adminTrashService: AdminTrashService) {

    @GetMapping
    fun list(): ApiResponse<List<TrashItemResponse>> = ApiResponse.ok(adminTrashService.list())
}
