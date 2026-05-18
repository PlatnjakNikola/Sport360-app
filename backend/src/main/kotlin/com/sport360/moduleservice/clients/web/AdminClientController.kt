package com.sport360.moduleservice.clients.web

import com.sport360.moduleservice.auth.web.MessageResponse
import com.sport360.moduleservice.clients.service.AdminClientService
import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.Pageables
import com.sport360.moduleservice.invites.service.InviteService
import com.sport360.moduleservice.invites.web.CreateClientInviteRequest
import com.sport360.moduleservice.invites.web.PendingClientInviteResponse
import com.sport360.moduleservice.security.CurrentUserService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/clients")
@PreAuthorize("hasRole('ADMIN')")
class AdminClientController(
    private val adminClientService: AdminClientService,
    private val inviteService: InviteService,
    private val currentUserService: CurrentUserService,
) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ApiResponse<PageResponse<ClientResponse>> =
        ApiResponse.ok(adminClientService.list(Pageables.of(page, limit)))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ApiResponse<ClientResponse> =
        ApiResponse.ok(adminClientService.get(id))

    @PostMapping("/invite")
    fun invite(@Valid @RequestBody request: CreateClientInviteRequest): ApiResponse<MessageResponse> {
        inviteService.createClientInvite(currentUserService.currentUserId(), request)
        return ApiResponse.ok(MessageResponse("Invite sent"))
    }

    @GetMapping("/invites")
    fun pendingInvites(): ApiResponse<List<PendingClientInviteResponse>> =
        ApiResponse.ok(inviteService.listPendingClientInvites())

    @PostMapping("/invites/{id}/resend")
    fun resendInvite(@PathVariable id: Long): ApiResponse<MessageResponse> {
        inviteService.resendClientInvite(currentUserService.currentUserId(), id)
        return ApiResponse.ok(MessageResponse("Invite resent"))
    }
}
