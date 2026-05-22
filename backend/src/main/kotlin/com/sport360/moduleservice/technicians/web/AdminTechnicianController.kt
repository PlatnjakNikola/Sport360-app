package com.sport360.moduleservice.technicians.web

import com.sport360.moduleservice.auth.web.MessageResponse
import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.Pageables
import com.sport360.moduleservice.invites.service.InviteService
import com.sport360.moduleservice.invites.web.CreateTechnicianInviteRequest
import com.sport360.moduleservice.invites.web.PendingTechnicianInviteResponse
import com.sport360.moduleservice.security.CurrentUserService
import com.sport360.moduleservice.technicians.service.AdminTechnicianService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/technicians")
@PreAuthorize("hasRole('ADMIN')")
class AdminTechnicianController(
    private val adminTechnicianService: AdminTechnicianService,
    private val inviteService: InviteService,
    private val currentUserService: CurrentUserService,
) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ApiResponse<PageResponse<TechnicianResponse>> =
        ApiResponse.ok(adminTechnicianService.list(Pageables.of(page, limit)))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ApiResponse<TechnicianResponse> =
        ApiResponse.ok(adminTechnicianService.get(id))

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateTechnicianRequest,
    ): ApiResponse<TechnicianResponse> =
        ApiResponse.ok(adminTechnicianService.update(currentUserService.currentUserId(), id, request))

    @PostMapping("/invite")
    fun invite(@Valid @RequestBody request: CreateTechnicianInviteRequest): ApiResponse<MessageResponse> {
        inviteService.createTechnicianInvite(currentUserService.currentUserId(), request)
        return ApiResponse.ok(MessageResponse("Invite sent"))
    }

    @GetMapping("/invites")
    fun pendingInvites(): ApiResponse<List<PendingTechnicianInviteResponse>> =
        ApiResponse.ok(inviteService.listPendingTechnicianInvites())

    @PostMapping("/invites/{id}/resend")
    fun resendInvite(@PathVariable id: Long): ApiResponse<MessageResponse> {
        inviteService.resendTechnicianInvite(currentUserService.currentUserId(), id)
        return ApiResponse.ok(MessageResponse("Invite resent"))
    }
}
