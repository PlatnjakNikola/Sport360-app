package com.sport360.moduleservice.packages

import com.sport360.moduleservice.notifications.repository.NotificationRepository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.audit.repository.AuditLogRepository
import com.sport360.moduleservice.clients.domain.Client
import com.sport360.moduleservice.clients.repository.ClientRepository
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.packages.repository.PackageStatusHistoryRepository
import com.sport360.moduleservice.security.JwtService
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
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
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
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
class ClientPackageIntegrationTest @Autowired constructor(
    private val notificationRepository: NotificationRepository,
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val clientRepository: ClientRepository,
    private val technicianRepository: TechnicianRepository,
    private val packageRepository: PackageRepository,
    private val packageStatusHistoryRepository: PackageStatusHistoryRepository,
    private val auditLogRepository: AuditLogRepository,
    transactionManager: PlatformTransactionManager,
) {

    private val tx = TransactionTemplate(transactionManager)

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    private lateinit var client1: Cookie
    private lateinit var client2: Cookie

    @BeforeEach
    fun setUp() {
        notificationRepository.deleteAll()
        packageStatusHistoryRepository.deleteAll()
        packageRepository.deleteAll()
        clientRepository.deleteAll()
        technicianRepository.deleteAll()
        auditLogRepository.deleteAll()
        userRepository.deleteAll()
        client1 = cookieFor(createClient("client1@test.local"))
        client2 = cookieFor(createClient("client2@test.local"))
    }

    @Test
    fun `client creates a package in created status`() {
        val result = createPackage(client1, "PKG-1")

        assertThat(result.response.status).isEqualTo(200)
        val data = body(result)["data"]
        assertThat(data["statusCode"].asText()).isEqualTo("created")
        assertThat(data["timeline"].size()).isEqualTo(1)
        assertThat(packageRepository.count()).isEqualTo(1)
    }

    @Test
    fun `duplicate package number is rejected`() {
        createPackage(client1, "PKG-DUP")
        val second = createPackage(client1, "PKG-DUP")
        assertThat(second.response.status).isEqualTo(409)
    }

    @Test
    fun `list returns only the client's own packages`() {
        createPackage(client1, "PKG-A")
        createPackage(client2, "PKG-B")

        val result = mockMvc.perform(get("/api/v1/client/packages").cookie(client1)).andReturn()

        val data = body(result)["data"]
        assertThat(data["pagination"]["total"].asInt()).isEqualTo(1)
        assertThat(data["items"][0]["packageNumber"].asText()).isEqualTo("PKG-A")
    }

    @Test
    fun `client cannot read another client's package`() {
        val id = packageId(createPackage(client2, "PKG-OTHER"))

        val result = mockMvc.perform(get("/api/v1/client/packages/$id").cookie(client1)).andReturn()

        assertThat(result.response.status).isEqualTo(404)
    }

    @Test
    fun `client can update tracking, note and description`() {
        val id = packageId(createPackage(client1, "PKG-EDIT"))

        val result = mockMvc.perform(
            patch("/api/v1/client/packages/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("note" to "Updated note", "outboundTrackingLink" to "https://track/1")))
                .cookie(client1),
        ).andReturn()

        assertThat(result.response.status).isEqualTo(200)
        val data = body(result)["data"]
        assertThat(data["note"].asText()).isEqualTo("Updated note")
        assertThat(data["outboundTrackingLink"].asText()).isEqualTo("https://track/1")
    }

    @Test
    fun `mark sent moves created to sent_to_service and blocks a second attempt`() {
        val id = packageId(createPackage(client1, "PKG-SEND"))

        val first = mockMvc.perform(post("/api/v1/client/packages/$id/mark-sent").cookie(client1)).andReturn()
        assertThat(first.response.status).isEqualTo(200)
        val data = body(first)["data"]
        assertThat(data["statusCode"].asText()).isEqualTo("sent_to_service")
        assertThat(data["timeline"].size()).isEqualTo(2)

        val second = mockMvc.perform(post("/api/v1/client/packages/$id/mark-sent").cookie(client1)).andReturn()
        assertThat(second.response.status).isEqualTo(409)
    }

    @Test
    fun `dashboard returns status counts for own packages`() {
        createPackage(client1, "PKG-D1")
        createPackage(client1, "PKG-D2")

        val result = mockMvc.perform(get("/api/v1/client/dashboard").cookie(client1)).andReturn()

        assertThat(result.response.status).isEqualTo(200)
        val counts = body(result)["data"]["statusCounts"]
        val created = counts.firstOrNull { it["code"].asText() == "created" }
        assertThat(created).isNotNull
        assertThat(created!!["count"].asInt()).isEqualTo(2)
    }

    @Test
    fun `client routes reject technicians and anonymous`() {
        assertThat(mockMvc.perform(get("/api/v1/client/packages")).andReturn().response.status).isEqualTo(401)

        val technician = userRepository.save(
            User(roleRepository.findByCode("technician")!!, "Tech", "tech@test.local", passwordEncoder.encode("password123")),
        )
        val technicianCookie = Cookie("access_token", jwtService.issueAccessToken(technician))
        assertThat(mockMvc.perform(get("/api/v1/client/packages").cookie(technicianCookie)).andReturn().response.status).isEqualTo(403)
    }

    // ----- helpers -----

    private fun createClient(email: String): User = requireNotNull(
        tx.execute {
            val role = roleRepository.findByCode("client") ?: error("client role missing")
            val user = userRepository.save(User(role, "Contact $email", email, passwordEncoder.encode("password123")))
            clientRepository.save(Client(user, "Company $email", null, null))
            user
        },
    )

    private fun cookieFor(user: User) = Cookie("access_token", jwtService.issueAccessToken(user))

    private fun createPackage(cookie: Cookie, number: String): MvcResult =
        mockMvc.perform(
            post("/api/v1/client/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("packageNumber" to number, "description" to "Some modules")))
                .cookie(cookie),
        ).andReturn()

    private fun packageId(result: MvcResult): Long = body(result)["data"]["id"].asLong()

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
