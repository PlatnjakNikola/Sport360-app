package com.sport360.moduleservice.publiclookup

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.audit.repository.AuditLogRepository
import com.sport360.moduleservice.clients.domain.Client
import com.sport360.moduleservice.clients.repository.ClientRepository
import com.sport360.moduleservice.modules.repository.ModuleRepairRepository
import com.sport360.moduleservice.modules.repository.ModuleRepository
import com.sport360.moduleservice.modules.repository.ModuleStatusHistoryRepository
import com.sport360.moduleservice.notifications.repository.NotificationRepository
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.packages.repository.PackageStatusHistoryRepository
import com.sport360.moduleservice.packages.repository.PackageStatusRepository
import com.sport360.moduleservice.ratelimit.RateLimiter
import com.sport360.moduleservice.security.JwtService
import com.sport360.moduleservice.servicecenters.domain.ServiceCenter
import com.sport360.moduleservice.servicecenters.repository.ServiceCenterRepository
import com.sport360.moduleservice.technicians.domain.Technician
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class PublicLookupIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val rateLimiter: RateLimiter,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val serviceCenterRepository: ServiceCenterRepository,
    private val technicianRepository: TechnicianRepository,
    private val clientRepository: ClientRepository,
    private val packageRepository: PackageRepository,
    private val packageStatusRepository: PackageStatusRepository,
    private val packageStatusHistoryRepository: PackageStatusHistoryRepository,
    private val moduleRepository: ModuleRepository,
    private val moduleRepairRepository: ModuleRepairRepository,
    private val moduleStatusHistoryRepository: ModuleStatusHistoryRepository,
    private val notificationRepository: NotificationRepository,
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

    private lateinit var adminCookie: Cookie
    private lateinit var techCookie: Cookie
    private lateinit var clientCookie: Cookie

    @BeforeEach
    fun setUp() {
        rateLimiter.clear()
        notificationRepository.deleteAll()
        moduleStatusHistoryRepository.deleteAll()
        moduleRepairRepository.deleteAll()
        moduleRepository.deleteAll()
        packageStatusHistoryRepository.deleteAll()
        packageRepository.deleteAll()
        technicianRepository.deleteAll()
        clientRepository.deleteAll()
        auditLogRepository.deleteAll()
        userRepository.deleteAll()
        serviceCenterRepository.deleteAll(serviceCenterRepository.findAll().filter { it.code != "ZAGREB" })

        val zagreb = serviceCenterRepository.findByCode("ZAGREB") ?: error("ZAGREB missing")
        adminCookie = cookieFor(createUser("admin", "admin@test.local"))
        techCookie = cookieFor(createTechnician("tech@test.local", zagreb))
        clientCookie = cookieFor(createClient("client@test.local"))
    }

    @Test
    fun `public history returns privacy-safe visits without auth`() {
        seedRepairedModule("MOD-PUB-1")
        val result = mockMvc.perform(get("/api/v1/public/modules/MOD-PUB-1/history")).andReturn()
        assertThat(result.response.status).isEqualTo(200)

        val items = body(result)["data"]
        assertThat(items.size()).isEqualTo(1)
        val visit = items[0]
        assertThat(visit["statusCode"].asText()).isEqualTo("repaired")
        assertThat(visit["problemTypeName"].asText()).isNotBlank()
        assertThat(visit["pixelsRepaired"].asInt()).isEqualTo(4)

        // privacy: the payload must not expose prices, names, package numbers or images
        val raw = result.response.contentAsString.lowercase()
        assertThat(raw).doesNotContain("price")
        assertThat(raw).doesNotContain("technician")
        assertThat(raw).doesNotContain("packagenumber")
        assertThat(raw).doesNotContain("companyname")
        assertThat(raw).doesNotContain("image")
    }

    @Test
    fun `unknown module number returns 404`() {
        assertThat(mockMvc.perform(get("/api/v1/public/modules/NOPE-1/history")).andReturn().response.status).isEqualTo(404)
    }

    @Test
    fun `public history is rate limited to 20 per minute per ip`() {
        seedRepairedModule("MOD-RL-1")
        repeat(20) {
            assertThat(mockMvc.perform(get("/api/v1/public/modules/MOD-RL-1/history")).andReturn().response.status).isEqualTo(200)
        }
        assertThat(mockMvc.perform(get("/api/v1/public/modules/MOD-RL-1/history")).andReturn().response.status).isEqualTo(429)
    }

    // ----- helpers -----

    private fun seedRepairedModule(moduleNumber: String) {
        val created = postJson("/api/v1/client/packages", mapOf("packageNumber" to "PKG-$moduleNumber", "description" to "m"), clientCookie)
        val packageId = body(created)["data"]["id"].asLong()
        postEmpty("/api/v1/client/packages/$packageId/mark-sent", clientCookie)
        overrideStatus(packageId, "on_service")
        val moduleId = body(
            postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to moduleNumber, "problemTypeId" to 1), techCookie),
        )["data"]["id"].asLong()
        postJson("/api/v1/technician/modules/$moduleId/repair", mapOf("decision" to "repaired", "pixelsRepaired" to 4, "chipsReplaced" to 1, "price" to 25.0), techCookie)
    }

    private fun overrideStatus(packageId: Long, code: String) {
        val statusId = packageStatusRepository.findByCode(code)!!.id
        mockMvc.perform(
            patch("/api/v1/admin/packages/$packageId/status").cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("statusId" to statusId))),
        ).andReturn()
    }

    private fun createUser(roleCode: String, email: String): User = requireNotNull(
        tx.execute {
            val role = roleRepository.findByCode(roleCode) ?: error("$roleCode role missing")
            userRepository.save(User(role, "$roleCode $email", email, passwordEncoder.encode("password123")))
        },
    )

    private fun createTechnician(email: String, serviceCenter: ServiceCenter): User = requireNotNull(
        tx.execute {
            val role = roleRepository.findByCode("technician") ?: error("technician role missing")
            val user = userRepository.save(User(role, "Tech $email", email, passwordEncoder.encode("password123")))
            technicianRepository.save(Technician(user, serviceCenter, "0911234567"))
            user
        },
    )

    private fun createClient(email: String): User = requireNotNull(
        tx.execute {
            val role = roleRepository.findByCode("client") ?: error("client role missing")
            val user = userRepository.save(User(role, "Client $email", email, passwordEncoder.encode("password123")))
            clientRepository.save(Client(user, "Company $email", null, null))
            user
        },
    )

    private fun cookieFor(user: User) = Cookie("access_token", jwtService.issueAccessToken(user))

    private fun postJson(path: String, payload: Any, cookie: Cookie): MvcResult =
        mockMvc.perform(
            post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)).cookie(cookie),
        ).andReturn()

    private fun postEmpty(path: String, cookie: Cookie): MvcResult = mockMvc.perform(post(path).cookie(cookie)).andReturn()

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
