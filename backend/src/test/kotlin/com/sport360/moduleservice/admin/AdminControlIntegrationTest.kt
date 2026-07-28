package com.sport360.moduleservice.admin

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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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
class AdminControlIntegrationTest @Autowired constructor(
    private val notificationRepository: NotificationRepository,
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jdbc: JdbcTemplate,
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
    private var zagrebId: Long = 0

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
        zagrebId = zagreb.id
        adminCookie = cookieFor(createUser("admin", "admin@test.local"))
        techCookie = cookieFor(createTechnician("tech@test.local", zagreb))
        clientCookie = cookieFor(createClient("client@test.local"))
    }

    @Test
    fun `restoring a package whose number was reused returns 409`() {
        val first = createPackage("PKG-1")
        adminDelete("/api/v1/admin/packages/$first")
        // number is now free — a new package can take it
        val second = createPackage("PKG-1")
        assertThat(second).isNotEqualTo(first)
        assertThat(adminPost("/api/v1/admin/packages/$first/restore").response.status).isEqualTo(409)
    }

    @Test
    fun `restoring a module whose number was reused in the package returns 409`() {
        val packageId = createPackage("PKG-2")
        overrideStatus(packageId, "on_service")
        val moduleId = scanModule(packageId, "M1")
        adminDelete("/api/v1/admin/modules/$moduleId")
        scanModule(packageId, "M1") // free number reused
        assertThat(adminPost("/api/v1/admin/modules/$moduleId/restore").response.status).isEqualTo(409)
    }

    @Test
    fun `status override sets the denormalized date and writes an admin_override audit row`() {
        val packageId = createPackage("PKG-3")
        val arrived = body(overrideStatusResult(packageId, "arrived"))["data"]
        assertThat(arrived["statusCode"].asText()).isEqualTo("arrived")
        assertThat(arrived["arrivedAt"].isNull).isFalse()
        val overrides = jdbc.queryForObject(
            "select count(*) from audit_logs where action_type = 'admin_override' and entity_id = ?",
            Int::class.java, packageId,
        )
        assertThat(overrides).isGreaterThanOrEqualTo(1)
    }

    @Test
    fun `bulk delete and bulk status affect every package`() {
        val a = createPackage("PKG-A")
        val b = createPackage("PKG-B")
        val onService = packageStatusRepository.findByCode("on_service")!!.id
        val bulkStatus = adminPost(
            "/api/v1/admin/packages/bulk-status",
            mapOf("packageIds" to listOf(a, b), "statusId" to onService),
        )
        assertThat(body(bulkStatus)["data"]["affected"].asInt()).isEqualTo(2)

        val bulkDelete = adminPost("/api/v1/admin/packages/bulk-delete", mapOf("packageIds" to listOf(a, b)))
        assertThat(body(bulkDelete)["data"]["affected"].asInt()).isEqualTo(2)
        // hidden by default, shown with includeDeleted
        assertThat(body(adminGet("/api/v1/admin/packages"))["data"]["pagination"]["total"].asInt()).isEqualTo(0)
        assertThat(body(adminGet("/api/v1/admin/packages?includeDeleted=true"))["data"]["pagination"]["total"].asInt()).isEqualTo(2)
    }

    @Test
    fun `admin correction module enforces a unique number within the package`() {
        val packageId = createPackage("PKG-4")
        val techId = userRepository.findByEmailIgnoreCase("tech@test.local")!!.id
        val created = adminPost(
            "/api/v1/admin/packages/$packageId/modules",
            mapOf("moduleNumber" to "C1", "problemTypeId" to 1, "assignedTechnicianId" to techId),
        )
        assertThat(created.response.status).isEqualTo(200)
        // duplicate number rejected
        assertThat(
            adminPost(
                "/api/v1/admin/packages/$packageId/modules",
                mapOf("moduleNumber" to "C1", "problemTypeId" to 1, "assignedTechnicianId" to techId),
            ).response.status,
        ).isEqualTo(409)
    }

    @Test
    fun `problem type create rejects duplicate code and deactivation hides it from technicians`() {
        val created = adminPost("/api/v1/admin/problem-types", mapOf("code" to "TESTPT", "name" to "Test PT", "sortOrder" to 99))
        val id = body(created)["data"]["id"].asInt()
        assertThat(technicianProblemTypeCodes()).contains("TESTPT")

        // duplicate code
        assertThat(
            adminPost("/api/v1/admin/problem-types", mapOf("code" to "TESTPT", "name" to "x", "sortOrder" to 98)).response.status,
        ).isEqualTo(409)

        // deactivate → hidden from technician form
        adminPatch("/api/v1/admin/problem-types/$id", mapOf("active" to false))
        assertThat(technicianProblemTypeCodes()).doesNotContain("TESTPT")
    }

    @Test
    fun `service center deactivation is blocked while active technicians are assigned`() {
        // ZAGREB has the test technician → cannot deactivate
        assertThat(adminPatch("/api/v1/admin/service-centers/$zagrebId", mapOf("active" to false)).response.status).isEqualTo(409)

        // a fresh center with no technicians can be created and deactivated
        val created = adminPost("/api/v1/admin/service-centers", mapOf("code" to "SPLIT", "name" to "Split", "city" to "Split"))
        val id = body(created)["data"]["id"].asLong()
        assertThat(adminPatch("/api/v1/admin/service-centers/$id", mapOf("active" to false)).response.status).isEqualTo(200)
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
        val created = postWith("/api/v1/client/packages", mapOf("packageNumber" to number, "description" to "modules"), clientCookie)
        return body(created)["data"]["id"].asLong()
    }

    private fun overrideStatusResult(packageId: Long, code: String): MvcResult {
        val statusId = packageStatusRepository.findByCode(code)!!.id
        return adminPatch("/api/v1/admin/packages/$packageId/status", mapOf("statusId" to statusId))
    }

    private fun overrideStatus(packageId: Long, code: String) {
        assertThat(overrideStatusResult(packageId, code).response.status).isEqualTo(200)
    }

    private fun scanModule(packageId: Long, number: String): Long = body(
        postWith("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to number, "problemTypeId" to 1), techCookie),
    )["data"]["id"].asLong()

    private fun technicianProblemTypeCodes(): List<String> =
        body(getWith("/api/v1/technician/problem-types", techCookie))["data"].map { it["code"].asText() }

    private fun adminDelete(path: String) {
        assertThat(mockMvc.perform(delete(path).cookie(adminCookie)).andReturn().response.status).isEqualTo(200)
    }

    private fun adminGet(path: String): MvcResult = getWith(path, adminCookie)

    private fun getWith(path: String, cookie: Cookie): MvcResult = mockMvc.perform(get(path).cookie(cookie)).andReturn()

    private fun adminPost(path: String, payload: Any? = null): MvcResult = postWith(path, payload, adminCookie)

    private fun postWith(path: String, payload: Any?, cookie: Cookie): MvcResult {
        val builder = post(path).cookie(cookie)
        if (payload != null) builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload))
        return mockMvc.perform(builder).andReturn()
    }

    private fun adminPatch(path: String, payload: Any): MvcResult = mockMvc.perform(
        patch(path).cookie(adminCookie).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)),
    ).andReturn()

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
