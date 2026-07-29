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
class InternalPackageIntegrationTest @Autowired constructor(
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
    fun `create internal package appears in the internal list`() {
        val created = createInternal("5mm novi")
        assertThat(created.response.status).isEqualTo(200)
        val data = body(created)["data"]
        assertThat(data["statusCode"].asText()).isEqualTo("created")
        assertThat(data["isInternal"].asBoolean()).isTrue()

        val list = getReq("/api/v1/technician/internal-packages", techCookie)
        assertThat(body(list)["data"]["pagination"]["total"].asInt()).isEqualTo(1)
    }

    @Test
    fun `technician fully processes an internal package and confirms arrival`() {
        val packageId = body(createInternal("rijeka moduli"))["data"]["id"].asLong()

        assertThat(statusAfterNext(packageId)).isEqualTo("sent_to_service") // technician drives created → sent
        assertThat(statusAfterNext(packageId)).isEqualTo("received_by_service")
        assertThat(statusAfterNext(packageId)).isEqualTo("on_service")

        val moduleId = body(
            postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to "IM1", "problemTypeId" to 1), techCookie),
        )["data"]["id"].asLong()
        postJson("/api/v1/technician/modules/$moduleId/repair", mapOf("decision" to "repaired", "price" to 10.0), techCookie)

        assertThat(statusAfterNext(packageId)).isEqualTo("repaired_waiting_shipment")
        assertThat(statusAfterNext(packageId)).isEqualTo("shipped_to_client")

        val arrived = postEmpty("/api/v1/technician/internal-packages/$packageId/confirm-arrival", techCookie)
        assertThat(body(arrived)["data"]["statusCode"].asText()).isEqualTo("arrived")
    }

    @Test
    fun `technician cannot advance an external created package`() {
        val externalId = createExternalPackage()
        assertThat(nextStatus(externalId).response.status).isEqualTo(409)
    }

    @Test
    fun `confirm-arrival rejects external packages`() {
        val externalId = createExternalPackage()
        assertThat(postEmpty("/api/v1/technician/internal-packages/$externalId/confirm-arrival", techCookie).response.status).isEqualTo(409)
    }

    // ----- helpers -----

    private fun createTechnician(email: String, serviceCenter: com.sport360.moduleservice.servicecenters.domain.ServiceCenter): User = requireNotNull(
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

    private fun createInternal(label: String): MvcResult =
        postJson("/api/v1/technician/internal-packages", mapOf("packageNumber" to label, "description" to "batch"), techCookie)

    private fun createExternalPackage(): Long {
        val created = postJson("/api/v1/client/packages", mapOf("packageNumber" to "EXT-1", "description" to "modules"), clientCookie)
        return body(created)["data"]["id"].asLong()
    }

    private fun nextStatus(packageId: Long): MvcResult =
        postJson("/api/v1/technician/packages/$packageId/next-status", emptyMap<String, Any>(), techCookie)

    private fun statusAfterNext(packageId: Long): String = body(nextStatus(packageId))["data"]["statusCode"].asText()

    private fun getReq(path: String, cookie: Cookie): MvcResult = mockMvc.perform(get(path).cookie(cookie)).andReturn()

    private fun postJson(path: String, payload: Any, cookie: Cookie): MvcResult =
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)).cookie(cookie)).andReturn()

    private fun postEmpty(path: String, cookie: Cookie): MvcResult = mockMvc.perform(post(path).cookie(cookie)).andReturn()

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
