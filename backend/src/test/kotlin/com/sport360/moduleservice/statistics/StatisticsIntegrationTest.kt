package com.sport360.moduleservice.statistics

import com.sport360.moduleservice.notifications.repository.NotificationRepository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.audit.repository.AuditLogRepository
import com.sport360.moduleservice.clients.domain.Client
import com.sport360.moduleservice.clients.repository.ClientRepository
import com.sport360.moduleservice.modules.repository.ModuleRepairRepository
import com.sport360.moduleservice.modules.repository.ModuleRepository
import com.sport360.moduleservice.modules.repository.ModuleStatusHistoryRepository
import com.sport360.moduleservice.packages.repository.PackageRepository
import com.sport360.moduleservice.packages.repository.PackageStatusHistoryRepository
import com.sport360.moduleservice.packages.repository.PackageStatusRepository
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
class StatisticsIntegrationTest @Autowired constructor(
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
    private val packageRepository: PackageRepository,
    private val packageStatusRepository: PackageStatusRepository,
    private val packageStatusHistoryRepository: PackageStatusHistoryRepository,
    private val moduleRepository: ModuleRepository,
    private val moduleRepairRepository: ModuleRepairRepository,
    private val moduleStatusHistoryRepository: ModuleStatusHistoryRepository,
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
    private var externalPackageId: Long = 0

    @BeforeEach
    fun setUp() {
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

        // One external package with a repaired module, and one internal package.
        externalPackageId = createPackage("EXT-1")
        overrideStatus(externalPackageId, "on_service")
        val moduleId = scanModule(externalPackageId, "M1")
        postJson("/api/v1/technician/modules/$moduleId/repair", mapOf("decision" to "repaired", "pixelsRepaired" to 4, "chipsReplaced" to 1, "price" to 30.0), techCookie)
        postJson("/api/v1/technician/internal-packages", mapOf("packageNumber" to "INT-1"), techCookie)
    }

    @Test
    fun `global statistics respect the type filter`() {
        assertThat(body(adminGet("/api/v1/admin/statistics?filter=all"))["data"]["totalPackages"].asInt()).isEqualTo(2)
        assertThat(body(adminGet("/api/v1/admin/statistics?filter=external"))["data"]["totalPackages"].asInt()).isEqualTo(1)
        assertThat(body(adminGet("/api/v1/admin/statistics?filter=internal"))["data"]["totalPackages"].asInt()).isEqualTo(1)

        val all = body(adminGet("/api/v1/admin/statistics?filter=all"))["data"]
        assertThat(all["repairedModules"].asInt()).isEqualTo(1)
        assertThat(all["modulesByProblemType"].size()).isGreaterThanOrEqualTo(1)
    }

    @Test
    fun `per-package statistics include totals and technician breakdown`() {
        val data = body(adminGet("/api/v1/admin/packages/$externalPackageId/statistics"))["data"]
        assertThat(data["repairedCount"].asInt()).isEqualTo(1)
        assertThat(data["totalPixels"].asInt()).isEqualTo(4)
        assertThat(data["totalChips"].asInt()).isEqualTo(1)
        assertThat(data["technicianBreakdown"].size()).isEqualTo(1)
        assertThat(data["technicianBreakdown"][0]["repairedCount"].asInt()).isEqualTo(1)
    }

    @Test
    fun `dashboard reports totals and pending invites`() {
        val data = body(adminGet("/api/v1/admin/dashboard"))["data"]
        assertThat(data["totalPackages"].asInt()).isEqualTo(2)
        assertThat(data["totalModules"].asInt()).isEqualTo(1)
        assertThat(data["repairedModules"].asInt()).isEqualTo(1)
        assertThat(data.has("pendingTechnicianInvites")).isTrue()
        assertThat(data["recentActivity"].isArray).isTrue()
    }

    @Test
    fun `csv export returns a csv document`() {
        val result = adminGet("/api/v1/admin/statistics/export.csv?filter=all")
        assertThat(result.response.status).isEqualTo(200)
        assertThat(result.response.contentType).startsWith("text/csv")
        assertThat(result.response.contentAsString).contains("Packages by status")
        assertThat(result.response.contentAsString).contains("Modules by problem type")
    }

    @Test
    fun `statistics require admin role`() {
        assertThat(mockMvc.perform(get("/api/v1/admin/statistics").cookie(techCookie)).andReturn().response.status).isEqualTo(403)
    }

    // ----- helpers -----

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

    private fun createPackage(number: String): Long {
        val created = postJson("/api/v1/client/packages", mapOf("packageNumber" to number, "description" to "modules"), clientCookie)
        return body(created)["data"]["id"].asLong()
    }

    private fun overrideStatus(packageId: Long, code: String) {
        val statusId = packageStatusRepository.findByCode(code)!!.id
        val result = mockMvc.perform(
            patch("/api/v1/admin/packages/$packageId/status").cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mapOf("statusId" to statusId))),
        ).andReturn()
        assertThat(result.response.status).isEqualTo(200)
    }

    private fun scanModule(packageId: Long, number: String): Long = body(
        postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to number, "problemTypeId" to 1), techCookie),
    )["data"]["id"].asLong()

    private fun adminGet(path: String): MvcResult = mockMvc.perform(get(path).cookie(adminCookie)).andReturn()

    private fun postJson(path: String, payload: Any, cookie: Cookie): MvcResult =
        mockMvc.perform(
            post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)).cookie(cookie),
        ).andReturn()

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
