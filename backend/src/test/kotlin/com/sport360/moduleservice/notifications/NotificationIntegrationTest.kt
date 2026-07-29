package com.sport360.moduleservice.notifications

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
class NotificationIntegrationTest @Autowired constructor(
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
    fun `status change notifies the client and the admin, and read state can be cleared`() {
        // client creates + sends package, technician advances it
        val packageId = createAndSendPackage()
        postEmpty("/api/v1/technician/packages/$packageId/next-status", techCookie) // received_by_service

        // client got a status-change notification
        assertThat(unreadCount(clientCookie)).isEqualTo(1)
        val items = body(getReq("/api/v1/client/notifications", clientCookie))["data"]["items"]
        assertThat(items.size()).isEqualTo(1)
        assertThat(items[0]["title"].asText()).contains("Received by service")
        val notifId = items[0]["id"].asLong()

        // admin received notifications too (package_created + sent + status change)
        assertThat(unreadCount(adminCookie)).isGreaterThanOrEqualTo(1)

        // a technician cannot mark the client's notification read
        assertThat(mockMvc.perform(patch("/api/v1/technician/notifications/$notifId/read").cookie(techCookie)).andReturn().response.status).isEqualTo(404)

        // client marks it read → unread count drops to 0
        assertThat(mockMvc.perform(patch("/api/v1/client/notifications/$notifId/read").cookie(clientCookie)).andReturn().response.status).isEqualTo(200)
        assertThat(unreadCount(clientCookie)).isEqualTo(0)
    }

    @Test
    fun `mark all read clears the unread count`() {
        val packageId = createAndSendPackage() // notifies admin twice (created + sent)
        postEmpty("/api/v1/technician/packages/$packageId/next-status", techCookie) // + status change
        assertThat(unreadCount(adminCookie)).isGreaterThanOrEqualTo(2)
        assertThat(postEmpty("/api/v1/admin/notifications/mark-all-read", adminCookie).response.status).isEqualTo(200)
        assertThat(unreadCount(adminCookie)).isEqualTo(0)
    }

    @Test
    fun `audit logs are listable and filterable`() {
        val packageId = createAndSendPackage()
        postEmpty("/api/v1/technician/packages/$packageId/next-status", techCookie)

        val byEntity = body(getReq("/api/v1/admin/audit-logs?entityType=package", adminCookie))["data"]
        assertThat(byEntity["pagination"]["total"].asInt()).isGreaterThanOrEqualTo(1)
        assertThat(byEntity["items"][0]["entityType"].asText()).isEqualTo("package")

        val byAction = body(getReq("/api/v1/admin/audit-logs?actionType=status_change", adminCookie))["data"]
        assertThat(byAction["pagination"]["total"].asInt()).isGreaterThanOrEqualTo(1)
        assertThat(byAction["items"][0]["changedByName"]).isNotNull
    }

    @Test
    fun `audit logs require admin`() {
        assertThat(getReq("/api/v1/admin/audit-logs", techCookie).response.status).isEqualTo(403)
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

    private fun createAndSendPackage(): Long {
        val created = postJson("/api/v1/client/packages", mapOf("packageNumber" to "PKG-1", "description" to "modules"), clientCookie)
        val id = body(created)["data"]["id"].asLong()
        postEmpty("/api/v1/client/packages/$id/mark-sent", clientCookie)
        return id
    }

    private fun unreadCount(cookie: Cookie): Int =
        body(getReq("/api/v1/client/notifications/unread-count", cookie))["data"]["count"].asInt()

    private fun getReq(path: String, cookie: Cookie): MvcResult = mockMvc.perform(get(path).cookie(cookie)).andReturn()

    private fun postJson(path: String, payload: Any, cookie: Cookie): MvcResult =
        mockMvc.perform(
            post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)).cookie(cookie),
        ).andReturn()

    private fun postEmpty(path: String, cookie: Cookie): MvcResult = mockMvc.perform(post(path).cookie(cookie)).andReturn()

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
