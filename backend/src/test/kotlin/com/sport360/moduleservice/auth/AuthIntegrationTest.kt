package com.sport360.moduleservice.auth

import com.sport360.moduleservice.notifications.repository.NotificationRepository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.auth.domain.MfaCode
import com.sport360.moduleservice.auth.repository.MfaCodeRepository
import com.sport360.moduleservice.auth.repository.PasswordResetTokenRepository
import com.sport360.moduleservice.auth.repository.RefreshTokenRepository
import com.sport360.moduleservice.audit.repository.AuditLogRepository
import com.sport360.moduleservice.ratelimit.RateLimiter
import com.sport360.moduleservice.security.JwtService
import com.sport360.moduleservice.testsupport.CapturingEmailService
import com.sport360.moduleservice.testsupport.TestEmailConfig
import com.sport360.moduleservice.users.domain.User
import com.sport360.moduleservice.users.repository.RoleRepository
import com.sport360.moduleservice.users.repository.UserRepository
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@Import(TestEmailConfig::class)
class AuthIntegrationTest @Autowired constructor(
    private val notificationRepository: NotificationRepository,
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val mfaCodeRepository: MfaCodeRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val auditLogRepository: AuditLogRepository,
    private val rateLimiter: RateLimiter,
    private val capturingEmail: CapturingEmailService,
) {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        private const val PASSWORD = "password123"
        private const val ADMIN_EMAIL = "admin@test.local"
        private const val TECH_EMAIL = "tech@test.local"
        private const val CLIENT_EMAIL = "client@test.local"
        private const val INACTIVE_EMAIL = "inactive@test.local"
    }

    private lateinit var admin: User

    @BeforeEach
    fun setUp() {
        notificationRepository.deleteAll()
        rateLimiter.clear()
        capturingEmail.lastMfaCode = null
        capturingEmail.lastResetLink = null
        refreshTokenRepository.deleteAll()
        passwordResetTokenRepository.deleteAll()
        mfaCodeRepository.deleteAll()
        auditLogRepository.deleteAll()
        userRepository.deleteAll()
        admin = createUser(ADMIN_EMAIL, PASSWORD, "admin")
        createUser(TECH_EMAIL, PASSWORD, "technician")
        createUser(CLIENT_EMAIL, PASSWORD, "client")
        createUser(INACTIVE_EMAIL, PASSWORD, "client", active = false)
    }

    @Test
    fun `technician login sets auth cookies and returns profile`() {
        val result = postJson("/api/v1/auth/login", mapOf("email" to TECH_EMAIL, "password" to PASSWORD))

        assertThat(result.response.status).isEqualTo(200)
        val data = body(result)["data"]
        assertThat(data["mfaRequired"].asBoolean()).isFalse()
        assertThat(data["user"]["role"].asText()).isEqualTo("technician")
        assertThat(result.response.getCookie("access_token")).isNotNull()
        assertThat(result.response.getCookie("refresh_token")).isNotNull()
    }

    @Test
    fun `admin login returns mfaToken and sets no cookies`() {
        val result = postJson("/api/v1/auth/login", mapOf("email" to ADMIN_EMAIL, "password" to PASSWORD))

        assertThat(result.response.status).isEqualTo(200)
        val data = body(result)["data"]
        assertThat(data["mfaRequired"].asBoolean()).isTrue()
        assertThat(data["mfaToken"].asText()).isNotBlank()
        assertThat(result.response.getCookie("access_token")).isNull()
        assertThat(result.response.getCookie("refresh_token")).isNull()
        assertThat(capturingEmail.lastMfaCode).isNotNull()
    }

    @Test
    fun `login with wrong password returns 401`() {
        val result = postJson("/api/v1/auth/login", mapOf("email" to TECH_EMAIL, "password" to "wrong-password"))

        assertThat(result.response.status).isEqualTo(401)
        assertThat(body(result)["error"]["code"].asText()).isEqualTo("UNAUTHORIZED")
    }

    @Test
    fun `inactive account cannot log in`() {
        val result = postJson("/api/v1/auth/login", mapOf("email" to INACTIVE_EMAIL, "password" to PASSWORD))

        assertThat(result.response.status).isEqualTo(401)
    }

    @Test
    fun `admin can complete MFA verification`() {
        val login = postJson("/api/v1/auth/login", mapOf("email" to ADMIN_EMAIL, "password" to PASSWORD))
        val mfaToken = body(login)["data"]["mfaToken"].asText()
        val code = capturingEmail.lastMfaCode!!

        val verify = postJson("/api/v1/auth/mfa/verify", mapOf("mfaToken" to mfaToken, "code" to code))

        assertThat(verify.response.status).isEqualTo(200)
        assertThat(body(verify)["data"]["user"]["role"].asText()).isEqualTo("admin")
        assertThat(verify.response.getCookie("access_token")).isNotNull()
        assertThat(verify.response.getCookie("refresh_token")).isNotNull()
    }

    @Test
    fun `MFA verification with wrong code returns 401`() {
        val login = postJson("/api/v1/auth/login", mapOf("email" to ADMIN_EMAIL, "password" to PASSWORD))
        val mfaToken = body(login)["data"]["mfaToken"].asText()
        val wrongCode = if (capturingEmail.lastMfaCode == "000000") "111111" else "000000"

        val verify = postJson("/api/v1/auth/mfa/verify", mapOf("mfaToken" to mfaToken, "code" to wrongCode))

        assertThat(verify.response.status).isEqualTo(401)
    }

    @Test
    fun `MFA verification is blocked after max attempts`() {
        val mfaToken = jwtService.issueMfaToken(admin.id)
        val mfa = MfaCode(admin.id, passwordEncoder.encode("654321"), OffsetDateTime.now().plusMinutes(10))
        repeat(5) { mfa.incrementAttempts() }
        mfaCodeRepository.save(mfa)

        val verify = postJson("/api/v1/auth/mfa/verify", mapOf("mfaToken" to mfaToken, "code" to "654321"))

        assertThat(verify.response.status).isEqualTo(401)
        assertThat(body(verify)["error"]["message"].asText()).contains("Too many attempts")
    }

    @Test
    fun `me returns profile with access cookie and 401 without`() {
        val (access, _) = loginCookies(TECH_EMAIL)

        val ok = mockMvc.perform(get("/api/v1/auth/me").cookie(access)).andReturn()
        assertThat(ok.response.status).isEqualTo(200)
        assertThat(body(ok)["data"]["email"].asText()).isEqualTo(TECH_EMAIL)

        val unauthorized = mockMvc.perform(get("/api/v1/auth/me")).andReturn()
        assertThat(unauthorized.response.status).isEqualTo(401)
    }

    @Test
    fun `refresh rotates the refresh token and revokes the old one`() {
        val (_, refresh) = loginCookies(TECH_EMAIL)

        val rotated = mockMvc.perform(post("/api/v1/auth/refresh-token").cookie(refresh)).andReturn()
        assertThat(rotated.response.status).isEqualTo(200)
        val newRefresh = rotated.response.getCookie("refresh_token")!!
        assertThat(newRefresh.value).isNotEqualTo(refresh.value)

        val reused = mockMvc.perform(post("/api/v1/auth/refresh-token").cookie(refresh)).andReturn()
        assertThat(reused.response.status).isEqualTo(401)
    }

    @Test
    fun `logout revokes the refresh token`() {
        val (_, refresh) = loginCookies(TECH_EMAIL)

        val logout = mockMvc.perform(post("/api/v1/auth/logout").cookie(refresh)).andReturn()
        assertThat(logout.response.status).isEqualTo(200)

        val reused = mockMvc.perform(post("/api/v1/auth/refresh-token").cookie(refresh)).andReturn()
        assertThat(reused.response.status).isEqualTo(401)
    }

    @Test
    fun `change password with wrong current password returns 400`() {
        val (access, refresh) = loginCookies(TECH_EMAIL)

        val result = mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("currentPassword" to "wrong", "newPassword" to "newpassword123")))
                .cookie(access, refresh),
        ).andReturn()

        assertThat(result.response.status).isEqualTo(400)
        assertThat(body(result)["error"]["code"].asText()).isEqualTo("VALIDATION_ERROR")
    }

    @Test
    fun `change password updates the password`() {
        val (access, refresh) = loginCookies(TECH_EMAIL)

        val change = mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("currentPassword" to PASSWORD, "newPassword" to "newpassword123")))
                .cookie(access, refresh),
        ).andReturn()
        assertThat(change.response.status).isEqualTo(200)

        assertThat(login(TECH_EMAIL, PASSWORD).response.status).isEqualTo(401)
        assertThat(login(TECH_EMAIL, "newpassword123").response.status).isEqualTo(200)
    }

    @Test
    fun `forgot and reset password flow works`() {
        val forgot = postJson("/api/v1/auth/forgot-password", mapOf("email" to CLIENT_EMAIL))
        assertThat(forgot.response.status).isEqualTo(200)
        val token = capturingEmail.lastResetLink!!.substringAfterLast("/")

        val reset = postJson("/api/v1/auth/reset-password", mapOf("token" to token, "newPassword" to "resetpass123"))
        assertThat(reset.response.status).isEqualTo(200)

        assertThat(login(CLIENT_EMAIL, PASSWORD).response.status).isEqualTo(401)
        assertThat(login(CLIENT_EMAIL, "resetpass123").response.status).isEqualTo(200)
    }

    @Test
    fun `forgot password for unknown email still returns 200`() {
        val result = postJson("/api/v1/auth/forgot-password", mapOf("email" to "nobody@test.local"))
        assertThat(result.response.status).isEqualTo(200)
    }

    @Test
    fun `reset password with invalid token returns 400`() {
        val result = postJson("/api/v1/auth/reset-password", mapOf("token" to "bogus-token", "newPassword" to "whatever123"))
        assertThat(result.response.status).isEqualTo(400)
    }

    @Test
    fun `role guard allows admin and forbids client`() {
        val noAuth = mockMvc.perform(get("/api/v1/test/admin-only")).andReturn()
        assertThat(noAuth.response.status).isEqualTo(401)

        val (clientAccess, _) = loginCookies(CLIENT_EMAIL)
        val forbidden = mockMvc.perform(get("/api/v1/test/admin-only").cookie(clientAccess)).andReturn()
        assertThat(forbidden.response.status).isEqualTo(403)

        val adminAccess = adminAccessCookie()
        val allowed = mockMvc.perform(get("/api/v1/test/admin-only").cookie(adminAccess)).andReturn()
        assertThat(allowed.response.status).isEqualTo(200)
    }

    @Test
    fun `login is rate limited per IP`() {
        repeat(5) {
            postJson("/api/v1/auth/login", mapOf("email" to TECH_EMAIL, "password" to "wrong-password"))
        }
        val sixth = postJson("/api/v1/auth/login", mapOf("email" to TECH_EMAIL, "password" to "wrong-password"))

        assertThat(sixth.response.status).isEqualTo(429)
        assertThat(body(sixth)["error"]["code"].asText()).isEqualTo("RATE_LIMITED")
    }

    // ----- helpers -----

    private fun createUser(email: String, password: String, roleCode: String, active: Boolean = true): User {
        val role = roleRepository.findByCode(roleCode) ?: error("Role $roleCode missing")
        val user = User(role, roleCode.replaceFirstChar { it.uppercase() }, email, passwordEncoder.encode(password))
        if (!active) user.isActive = false
        return userRepository.save(user)
    }

    private fun postJson(path: String, payload: Any): MvcResult =
        mockMvc.perform(
            post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        ).andReturn()

    private fun login(email: String, password: String): MvcResult =
        postJson("/api/v1/auth/login", mapOf("email" to email, "password" to password))

    private fun loginCookies(email: String): Pair<Cookie, Cookie> {
        val result = login(email, PASSWORD)
        return result.response.getCookie("access_token")!! to result.response.getCookie("refresh_token")!!
    }

    private fun adminAccessCookie(): Cookie {
        val loginResult = login(ADMIN_EMAIL, PASSWORD)
        val mfaToken = body(loginResult)["data"]["mfaToken"].asText()
        val code = capturingEmail.lastMfaCode!!
        val verify = postJson("/api/v1/auth/mfa/verify", mapOf("mfaToken" to mfaToken, "code" to code))
        return verify.response.getCookie("access_token")!!
    }

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
