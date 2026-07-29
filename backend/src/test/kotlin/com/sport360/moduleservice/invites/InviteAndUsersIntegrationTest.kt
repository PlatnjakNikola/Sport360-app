package com.sport360.moduleservice.invites

import com.sport360.moduleservice.notifications.repository.NotificationRepository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.auth.repository.MfaCodeRepository
import com.sport360.moduleservice.auth.repository.PasswordResetTokenRepository
import com.sport360.moduleservice.auth.repository.RefreshTokenRepository
import com.sport360.moduleservice.audit.repository.AuditLogRepository
import com.sport360.moduleservice.clients.repository.ClientRepository
import com.sport360.moduleservice.invites.repository.ClientInviteTokenRepository
import com.sport360.moduleservice.invites.repository.TechnicianInviteTokenRepository
import com.sport360.moduleservice.ratelimit.RateLimiter
import com.sport360.moduleservice.security.JwtService
import com.sport360.moduleservice.servicecenters.repository.ServiceCenterRepository
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@Import(TestEmailConfig::class)
class InviteAndUsersIntegrationTest @Autowired constructor(
    private val notificationRepository: NotificationRepository,
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val serviceCenterRepository: ServiceCenterRepository,
    private val technicianRepository: TechnicianRepository,
    private val clientRepository: ClientRepository,
    private val technicianInviteTokenRepository: TechnicianInviteTokenRepository,
    private val clientInviteTokenRepository: ClientInviteTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val mfaCodeRepository: MfaCodeRepository,
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
    }

    private lateinit var admin: User
    private var serviceCenterId: Long = 0

    @BeforeEach
    fun setUp() {
        notificationRepository.deleteAll()
        rateLimiter.clear()
        capturingEmail.lastInviteLink = null
        technicianRepository.deleteAll()
        clientRepository.deleteAll()
        technicianInviteTokenRepository.deleteAll()
        clientInviteTokenRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        mfaCodeRepository.deleteAll()
        passwordResetTokenRepository.deleteAll()
        auditLogRepository.deleteAll()
        userRepository.deleteAll()
        admin = createUser("admin@test.local", "admin")
        serviceCenterId = serviceCenterRepository.findAll().first().id!!
    }

    @Test
    fun `admin creates a technician invite and emails a link`() {
        val result = postJson("/api/v1/admin/technicians/invite", technicianInvitePayload("tech@test.local"), adminCookie())

        assertThat(result.response.status).isEqualTo(200)
        assertThat(capturingEmail.lastInviteLink).isNotNull()
        assertThat(technicianInviteTokenRepository.count()).isEqualTo(1)
    }

    @Test
    fun `duplicate invite email is rejected`() {
        postJson("/api/v1/admin/technicians/invite", technicianInvitePayload("dup@test.local"), adminCookie())
        val second = postJson("/api/v1/admin/clients/invite", clientInvitePayload("dup@test.local"), adminCookie())

        assertThat(second.response.status).isEqualTo(409)
    }

    @Test
    fun `validate returns technician prefill`() {
        postJson("/api/v1/admin/technicians/invite", technicianInvitePayload("tech@test.local"), adminCookie())
        val token = tokenFromLink()

        val result = mockMvc.perform(get("/api/v1/auth/invite/$token")).andReturn()

        assertThat(result.response.status).isEqualTo(200)
        val data = body(result)["data"]
        assertThat(data["type"].asText()).isEqualTo("technician")
        assertThat(data["email"].asText()).isEqualTo("tech@test.local")
        assertThat(data["serviceCenterName"].asText()).isEqualTo("Zagreb")
    }

    @Test
    fun `accept technician invite creates an account that can log in`() {
        postJson("/api/v1/admin/technicians/invite", technicianInvitePayload("tech@test.local"), adminCookie())
        val token = tokenFromLink()

        val accept = postJson("/api/v1/auth/accept-invite", mapOf("token" to token, "password" to PASSWORD))
        assertThat(accept.response.status).isEqualTo(200)
        assertThat(technicianRepository.count()).isEqualTo(1)

        val login = login("tech@test.local", PASSWORD)
        assertThat(login.response.status).isEqualTo(200)
        assertThat(body(login)["data"]["user"]["role"].asText()).isEqualTo("technician")

        val reuse = postJson("/api/v1/auth/accept-invite", mapOf("token" to token, "password" to PASSWORD))
        assertThat(reuse.response.status).isEqualTo(400)
    }

    @Test
    fun `accept client invite creates a client account`() {
        postJson("/api/v1/admin/clients/invite", clientInvitePayload("client@test.local"), adminCookie())
        val token = tokenFromLink()

        val accept = postJson("/api/v1/auth/accept-invite", mapOf("token" to token, "password" to PASSWORD))
        assertThat(accept.response.status).isEqualTo(200)
        assertThat(clientRepository.count()).isEqualTo(1)

        val login = login("client@test.local", PASSWORD)
        assertThat(body(login)["data"]["user"]["role"].asText()).isEqualTo("client")
    }

    @Test
    fun `resend invalidates the old link and issues a new one`() {
        postJson("/api/v1/admin/technicians/invite", technicianInvitePayload("tech@test.local"), adminCookie())
        val oldToken = tokenFromLink()
        val inviteId = inviteIdFromPending()

        postJson("/api/v1/admin/technicians/invites/$inviteId/resend", emptyMap<String, Any>(), adminCookie())
        val newToken = tokenFromLink()

        assertThat(newToken).isNotEqualTo(oldToken)
        assertThat(mockMvc.perform(get("/api/v1/auth/invite/$oldToken")).andReturn().response.status).isEqualTo(404)
        assertThat(mockMvc.perform(get("/api/v1/auth/invite/$newToken")).andReturn().response.status).isEqualTo(200)
    }

    @Test
    fun `deactivating a technician blocks login`() {
        postJson("/api/v1/admin/technicians/invite", technicianInvitePayload("tech@test.local"), adminCookie())
        postJson("/api/v1/auth/accept-invite", mapOf("token" to tokenFromLink(), "password" to PASSWORD))
        val userId = userRepository.findByEmailIgnoreCase("tech@test.local")!!.id

        val update = mockMvc.perform(
            patch("/api/v1/admin/technicians/$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("isActive" to false)))
                .cookie(adminCookie()),
        ).andReturn()
        assertThat(update.response.status).isEqualTo(200)

        assertThat(login("tech@test.local", PASSWORD).response.status).isEqualTo(401)
    }

    @Test
    fun `technician list is paginated`() {
        postJson("/api/v1/admin/technicians/invite", technicianInvitePayload("tech@test.local"), adminCookie())
        postJson("/api/v1/auth/accept-invite", mapOf("token" to tokenFromLink(), "password" to PASSWORD))

        val result = mockMvc.perform(get("/api/v1/admin/technicians").cookie(adminCookie())).andReturn()

        assertThat(result.response.status).isEqualTo(200)
        val data = body(result)["data"]
        assertThat(data["items"].size()).isEqualTo(1)
        assertThat(data["pagination"]["total"].asInt()).isEqualTo(1)
    }

    @Test
    fun `service centers list is available to admin`() {
        val result = mockMvc.perform(get("/api/v1/admin/service-centers").cookie(adminCookie())).andReturn()

        assertThat(result.response.status).isEqualTo(200)
        assertThat(body(result)["data"][0]["code"].asText()).isEqualTo("ZAGREB")
    }

    @Test
    fun `admin endpoints reject non-admins and anonymous`() {
        assertThat(mockMvc.perform(get("/api/v1/admin/technicians")).andReturn().response.status).isEqualTo(401)

        val technician = createUser("tech2@test.local", "technician")
        val cookie = Cookie("access_token", jwtService.issueAccessToken(technician))
        assertThat(mockMvc.perform(get("/api/v1/admin/technicians").cookie(cookie)).andReturn().response.status).isEqualTo(403)
    }

    @Test
    fun `accept invite with invalid token returns 400`() {
        val result = postJson("/api/v1/auth/accept-invite", mapOf("token" to "bogus", "password" to PASSWORD))
        assertThat(result.response.status).isEqualTo(400)
    }

    // ----- helpers -----

    private fun createUser(email: String, roleCode: String): User {
        val role = roleRepository.findByCode(roleCode) ?: error("Role $roleCode missing")
        return userRepository.save(User(role, roleCode.replaceFirstChar { it.uppercase() }, email, passwordEncoder.encode(PASSWORD)))
    }

    private fun adminCookie() = Cookie("access_token", jwtService.issueAccessToken(admin))

    private fun technicianInvitePayload(email: String) =
        mapOf("email" to email, "name" to "New Tech", "serviceCenterId" to serviceCenterId, "phone" to "0911234567")

    private fun clientInvitePayload(email: String) =
        mapOf("email" to email, "contactName" to "Jane Doe", "companyName" to "Acme d.o.o.", "contactPhone" to "0991234567", "address" to "Ilica 1")

    private fun postJson(path: String, payload: Any, vararg cookies: Cookie): MvcResult {
        val builder = post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload))
        cookies.forEach { builder.cookie(it) }
        return mockMvc.perform(builder).andReturn()
    }

    private fun login(email: String, password: String): MvcResult =
        postJson("/api/v1/auth/login", mapOf("email" to email, "password" to password))

    private fun tokenFromLink(): String = capturingEmail.lastInviteLink!!.substringAfterLast("/")

    private fun inviteIdFromPending(): Long {
        val result = mockMvc.perform(get("/api/v1/admin/technicians/invites").cookie(adminCookie())).andReturn()
        return body(result)["data"][0]["id"].asLong()
    }

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
