package com.sport360.moduleservice.auth.web

import com.fasterxml.jackson.annotation.JsonInclude

data class UserProfileResponse(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
)

/** Login / MFA-verify result. For admins the first step returns `mfaRequired=true` + `mfaToken`. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LoginResponse(
    val mfaRequired: Boolean,
    val mfaToken: String? = null,
    val user: UserProfileResponse? = null,
)

data class MessageResponse(val message: String)
