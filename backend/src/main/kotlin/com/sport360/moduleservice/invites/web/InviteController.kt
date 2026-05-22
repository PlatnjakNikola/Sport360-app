package com.sport360.moduleservice.invites.web

import com.sport360.moduleservice.auth.web.MessageResponse
import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.invites.service.InviteService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Public endpoints for validating and accepting an invite (no auth). */
@RestController
@RequestMapping("/api/v1/auth")
class InviteController(private val inviteService: InviteService) {

    @GetMapping("/invite/{token}")
    fun validate(@PathVariable token: String): ApiResponse<InviteValidationResponse> =
        ApiResponse.ok(inviteService.validate(token))

    @PostMapping("/accept-invite")
    fun accept(@Valid @RequestBody request: AcceptInviteRequest): ApiResponse<MessageResponse> {
        inviteService.accept(request.token, request.password)
        return ApiResponse.ok(MessageResponse("Account created. You can now sign in."))
    }
}
