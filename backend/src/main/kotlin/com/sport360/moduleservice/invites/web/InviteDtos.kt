package com.sport360.moduleservice.invites.web

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class CreateTechnicianInviteRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val name: String,
    @field:NotNull val serviceCenterId: Long?,
    val phone: String? = null,
)

data class CreateClientInviteRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val contactName: String,
    @field:NotBlank val companyName: String,
    val contactPhone: String? = null,
    val address: String? = null,
)

data class AcceptInviteRequest(
    @field:NotBlank val token: String,
    @field:NotBlank
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,
)

/** Prefill data for the accept-invite screen. Fields not relevant to the type are omitted. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InviteValidationResponse(
    val type: String,
    val email: String,
    val name: String? = null,
    val serviceCenterId: Long? = null,
    val serviceCenterName: String? = null,
    val companyName: String? = null,
    val contactPhone: String? = null,
    val address: String? = null,
)

data class PendingTechnicianInviteResponse(
    val id: Long,
    val email: String,
    val name: String,
    val serviceCenterId: Long,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
)

data class PendingClientInviteResponse(
    val id: Long,
    val email: String,
    val contactName: String,
    val companyName: String,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
)
