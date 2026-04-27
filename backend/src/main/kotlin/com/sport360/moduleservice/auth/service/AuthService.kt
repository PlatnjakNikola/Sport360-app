package com.sport360.moduleservice.auth.service

import com.sport360.moduleservice.common.UnauthorizedException
import com.sport360.moduleservice.security.JwtService
import com.sport360.moduleservice.users.domain.User
import com.sport360.moduleservice.users.repository.UserRepository
import com.sport360.moduleservice.auth.web.LoginResponse
import com.sport360.moduleservice.auth.web.UserProfileResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Login (with admin MFA split), MFA verify/resend, current user, logout. */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val tokenService: TokenService,
    private val mfaService: MfaService,
) {

    // Precomputed valid BCrypt hash to equalize timing when the email is unknown.
    private val dummyHash: String = passwordEncoder.encode("timing-equalizer-placeholder")

    @Transactional
    fun login(email: String, password: String, response: HttpServletResponse): LoginResponse {
        val invalidCredentials = UnauthorizedException("Invalid email or password")
        val user = userRepository.findByEmailIgnoreCase(email)
        if (user == null) {
            passwordEncoder.matches(password, dummyHash)
            throw invalidCredentials
        }
        if (!passwordEncoder.matches(password, user.passwordHash)) throw invalidCredentials
        if (!user.isActive) throw UnauthorizedException("Account is inactive")

        return if (user.role.code == ROLE_ADMIN) {
            mfaService.createAndSend(user)
            LoginResponse(mfaRequired = true, mfaToken = jwtService.issueMfaToken(user.id))
        } else {
            tokenService.issueAuthCookies(user, response)
            LoginResponse(mfaRequired = false, user = user.toProfile())
        }
    }

    @Transactional
    fun verifyMfa(mfaToken: String, code: String, response: HttpServletResponse): LoginResponse {
        val userId = jwtService.parseMfaToken(mfaToken)
        val user = userRepository.findById(userId).orElseThrow { UnauthorizedException("Invalid login session") }
        if (!user.isActive) throw UnauthorizedException("Account is inactive")
        mfaService.verify(userId, code)
        tokenService.issueAuthCookies(user, response)
        return LoginResponse(mfaRequired = false, user = user.toProfile())
    }

    @Transactional
    fun resendMfa(mfaToken: String) {
        val userId = jwtService.parseMfaToken(mfaToken)
        val user = userRepository.findById(userId).orElseThrow { UnauthorizedException("Invalid login session") }
        mfaService.resend(user)
    }

    fun me(userId: Long): UserProfileResponse =
        userRepository.findById(userId).orElseThrow { UnauthorizedException() }.toProfile()

    fun logout(rawRefresh: String?, response: HttpServletResponse) = tokenService.logout(rawRefresh, response)

    private fun User.toProfile() = UserProfileResponse(id, name, email, role.code)

    private companion object {
        const val ROLE_ADMIN = "admin"
    }
}
