package com.sport360.moduleservice.technicianworkflow

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class TechnicianWorkflowIntegrationTest @Autowired constructor(
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
        techCookie = cookieFor(createTechnician("tech@test.local", zagreb))
        clientCookie = cookieFor(createClient("client@test.local"))
    }

    @Test
    fun `full package lifecycle from sent to arrived`() {
        val packageId = createAndSendPackage()

        // Technician sees it in the active list
        val active = getReq("/api/v1/technician/packages/active", techCookie)
        assertThat(body(active)["data"]["pagination"]["total"].asInt()).isEqualTo(1)

        // receive → on_service
        assertThat(statusAfterNext(packageId)).isEqualTo("received_by_service")
        assertThat(getDetail(packageId)["receivedAt"].isNull).isFalse()
        assertThat(statusAfterNext(packageId)).isEqualTo("on_service")

        // client module view is locked while on service
        assertThat(getReq("/api/v1/client/packages/$packageId/modules", clientCookie).response.status).isEqualTo(403)

        // scan a module
        val moduleId = body(
            postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to "M1", "problemTypeId" to 1), techCookie),
        )["data"]["id"].asLong()

        // finishing service is blocked while a module is unfinished
        assertThat(nextStatus(packageId).response.status).isEqualTo(409)

        // repair the module
        val repair = postJson(
            "/api/v1/technician/modules/$moduleId/repair",
            mapOf("decision" to "repaired", "pixelsRepaired" to 5, "chipsReplaced" to 1, "price" to 25.0),
            techCookie,
        )
        assertThat(body(repair)["data"]["statusCode"].asText()).isEqualTo("repaired")

        // a second repair on the same module conflicts
        assertThat(
            postJson("/api/v1/technician/modules/$moduleId/repair", mapOf("decision" to "repaired"), techCookie).response.status,
        ).isEqualTo(409)

        // finish service now succeeds, then ship
        assertThat(statusAfterNext(packageId)).isEqualTo("repaired_waiting_shipment")
        val shipped = postJson(
            "/api/v1/technician/packages/$packageId/next-status",
            mapOf("returnTrackingLink" to "https://ret/1"),
            techCookie,
        )
        assertThat(body(shipped)["data"]["statusCode"].asText()).isEqualTo("shipped_to_client")

        // technician cannot advance a shipped package
        assertThat(nextStatus(packageId).response.status).isEqualTo(409)

        // client confirms arrival
        val arrived = postEmpty("/api/v1/client/packages/$packageId/confirm-arrival", clientCookie)
        assertThat(body(arrived)["data"]["statusCode"].asText()).isEqualTo("arrived")

        // client now sees modules, without technician name
        val modules = getReq("/api/v1/client/packages/$packageId/modules", clientCookie)
        assertThat(modules.response.status).isEqualTo(200)
        val items = body(modules)["data"]["items"]
        assertThat(items.size()).isEqualTo(1)
        assertThat(items[0].has("technicianName")).isFalse()
        assertThat(body(getReq("/api/v1/client/modules/$moduleId", clientCookie))["data"].has("technicianName")).isFalse()

        // the technician is notified that the package arrived (not just the admin)
        val techNotifications = body(getReq("/api/v1/technician/notifications", techCookie))["data"]["items"]
        assertThat(techNotifications.map { it["title"].asText() }).anyMatch { it.contains("Arrived") }
    }

    @Test
    fun `module create requires on_service and a unique number`() {
        val packageId = createAndSendPackage()
        // still sent_to_service → cannot add modules
        assertThat(
            postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to "M1", "problemTypeId" to 1), techCookie).response.status,
        ).isEqualTo(409)

        statusAfterNext(packageId) // received
        statusAfterNext(packageId) // on_service

        assertThat(postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to "M1", "problemTypeId" to 1), techCookie).response.status).isEqualTo(200)
        // duplicate number
        assertThat(postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to "M1", "problemTypeId" to 1), techCookie).response.status).isEqualTo(409)
        // unknown problem type
        assertThat(postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to "M2", "problemTypeId" to 999), techCookie).response.status).isEqualTo(400)
    }

    @Test
    fun `find module in package returns the match or nothing`() {
        val packageId = createAndSendPackage()
        statusAfterNext(packageId) // received
        statusAfterNext(packageId) // on_service
        val moduleId = body(
            postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to "M1", "problemTypeId" to 1), techCookie),
        )["data"]["id"].asLong()

        // existing module is found, with enough to route to its repair screen
        val found = getReq("/api/v1/technician/packages/$packageId/modules/find?number=M1", techCookie)
        assertThat(found.response.status).isEqualTo(200)
        assertThat(body(found)["data"]["id"].asLong()).isEqualTo(moduleId)
        assertThat(body(found)["data"]["statusCode"].asText()).isEqualTo("waiting_for_repair")

        // unknown number resolves to no data (caller offers to create a service request)
        val missing = getReq("/api/v1/technician/packages/$packageId/modules/find?number=NOPE", techCookie)
        assertThat(missing.response.status).isEqualTo(200)
        assertThat(body(missing).has("data")).isFalse()

        // a technician from another center cannot probe the package
        val rijeka = serviceCenterRepository.save(ServiceCenter("RIJEKA", "Rijeka", "Croatia", "Rijeka"))
        val otherCookie = cookieFor(createTechnician("tech3@test.local", rijeka))
        assertThat(getReq("/api/v1/technician/packages/$packageId/modules/find?number=M1", otherCookie).response.status).isEqualTo(404)
    }

    @Test
    fun `technician from another service center cannot see the package`() {
        val packageId = createAndSendPackage()
        val rijeka = serviceCenterRepository.save(ServiceCenter("RIJEKA", "Rijeka", "Croatia", "Rijeka"))
        val otherCookie = cookieFor(createTechnician("tech2@test.local", rijeka))

        assertThat(getReq("/api/v1/technician/packages/$packageId", otherCookie).response.status).isEqualTo(404)
    }

    @Test
    fun `role separation is enforced`() {
        assertThat(getReq("/api/v1/technician/dashboard", clientCookie).response.status).isEqualTo(403)
        assertThat(getReq("/api/v1/client/packages", techCookie).response.status).isEqualTo(403)
        assertThat(mockMvc.perform(get("/api/v1/technician/dashboard")).andReturn().response.status).isEqualTo(401)
    }

    @Test
    fun `technician dashboard responds`() {
        val result = getReq("/api/v1/technician/dashboard", techCookie)
        assertThat(result.response.status).isEqualTo(200)
        assertThat(body(result)["data"]["personalStats"]).isNotNull
    }

    // ----- helpers -----

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

    private fun nextStatus(packageId: Long): MvcResult =
        postJson("/api/v1/technician/packages/$packageId/next-status", emptyMap<String, Any>(), techCookie)

    private fun statusAfterNext(packageId: Long): String = body(nextStatus(packageId))["data"]["statusCode"].asText()

    private fun getDetail(packageId: Long): JsonNode = body(getReq("/api/v1/technician/packages/$packageId", techCookie))["data"]

    private fun getReq(path: String, cookie: Cookie): MvcResult =
        mockMvc.perform(get(path).cookie(cookie)).andReturn()

    private fun postJson(path: String, payload: Any, cookie: Cookie): MvcResult =
        mockMvc.perform(
            post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)).cookie(cookie),
        ).andReturn()

    private fun postEmpty(path: String, cookie: Cookie): MvcResult =
        mockMvc.perform(post(path).cookie(cookie)).andReturn()

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
