package com.sport360.moduleservice.images

import com.sport360.moduleservice.notifications.repository.NotificationRepository

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.audit.repository.AuditLogRepository
import com.sport360.moduleservice.clients.domain.Client
import com.sport360.moduleservice.clients.repository.ClientRepository
import com.sport360.moduleservice.images.service.ImageService
import com.sport360.moduleservice.modules.repository.ModuleImageRepository
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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ImageIntegrationTest @Autowired constructor(
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
    private val moduleImageRepository: ModuleImageRepository,
    private val moduleRepairRepository: ModuleRepairRepository,
    private val moduleStatusHistoryRepository: ModuleStatusHistoryRepository,
    private val auditLogRepository: AuditLogRepository,
    private val imageService: ImageService,
    transactionManager: PlatformTransactionManager,
) {

    private val tx = TransactionTemplate(transactionManager)

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        val storageDir: Path = Files.createTempDirectory("image-test-store")

        @JvmStatic
        @DynamicPropertySource
        fun storageProps(registry: DynamicPropertyRegistry) {
            registry.add("app.image.storage-path") { storageDir.toString() }
        }
    }

    private lateinit var techCookie: Cookie
    private lateinit var clientCookie: Cookie

    @BeforeEach
    fun setUp() {
        notificationRepository.deleteAll()
        moduleImageRepository.deleteAll()
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
    fun `upload validation, serving access checks, and visibility gating`() {
        val packageId = createAndSendPackage()
        statusAfterNext(packageId) // received
        statusAfterNext(packageId) // on_service
        val moduleId = scanModule(packageId, "M1")

        // upload one image
        val uploaded = uploadImages(moduleId, techCookie, png("a.png"))
        assertThat(uploaded.response.status).isEqualTo(200)
        val imageId = body(uploaded)["data"][0]["id"].asLong()
        assertThat(body(uploaded)["data"][0]["url"].asText()).isEqualTo("/api/v1/images/$imageId")

        // module detail (technician) now carries the image
        val detail = getReq("/api/v1/technician/modules/$moduleId", techCookie)
        assertThat(body(detail)["data"]["images"].size()).isEqualTo(1)

        // adding 5 more would exceed the max of 5
        assertThat(
            uploadImages(moduleId, techCookie, png("b.png"), png("c.png"), png("d.png"), png("e.png"), png("f.png"))
                .response.status,
        ).isEqualTo(409)

        // unsupported MIME type is rejected
        assertThat(
            uploadImages(moduleId, techCookie, MockMultipartFile("files", "x.txt", "text/plain", byteArrayOf(1)))
                .response.status,
        ).isEqualTo(400)

        // serving: owning technician can view
        val served = getReq("/api/v1/images/$imageId", techCookie)
        assertThat(served.response.status).isEqualTo(200)
        assertThat(served.response.contentType).startsWith("image/png")

        // a technician from another service center cannot
        val rijeka = serviceCenterRepository.save(ServiceCenter("RIJEKA", "Rijeka", "Croatia", "Rijeka"))
        val otherTech = cookieFor(createTechnician("tech2@test.local", rijeka))
        assertThat(getReq("/api/v1/images/$imageId", otherTech).response.status).isEqualTo(404)

        // the owning client cannot view while the package is still on service (locked)
        assertThat(getReq("/api/v1/images/$imageId", clientCookie).response.status).isEqualTo(403)

        // drive the package to arrived
        repair(moduleId)
        statusAfterNext(packageId) // repaired_waiting_shipment
        ship(packageId) // shipped_to_client
        postEmpty("/api/v1/client/packages/$packageId/confirm-arrival", clientCookie)

        // now the owning client can view
        assertThat(getReq("/api/v1/images/$imageId", clientCookie).response.status).isEqualTo(200)
        // a different client cannot
        val otherClient = cookieFor(createClient("client2@test.local"))
        assertThat(getReq("/api/v1/images/$imageId", otherClient).response.status).isEqualTo(404)
        // the admin can view any image
        val adminCookie = cookieFor(createAdmin("admin@test.local"))
        assertThat(getReq("/api/v1/images/$imageId", adminCookie).response.status).isEqualTo(200)
    }

    @Test
    fun `expired images are cleaned up after the retention window`() {
        val packageId = createAndSendPackage()
        statusAfterNext(packageId) // received
        statusAfterNext(packageId) // on_service
        val moduleId = scanModule(packageId, "M1")
        uploadImages(moduleId, techCookie, png("a.png"))
        repair(moduleId)
        statusAfterNext(packageId) // repaired_waiting_shipment
        ship(packageId) // shipped_to_client
        postEmpty("/api/v1/client/packages/$packageId/confirm-arrival", clientCookie)
        assertThat(moduleImageRepository.count()).isEqualTo(1)

        // backdate the arrival beyond the 30-day window
        tx.execute {
            val pkg = packageRepository.findById(packageId).orElseThrow()
            pkg.arrivedAt = OffsetDateTime.now().minusDays(40)
            packageRepository.save(pkg)
        }

        assertThat(imageService.cleanupExpired()).isEqualTo(1)
        assertThat(moduleImageRepository.count()).isEqualTo(0)
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

    private fun createAdmin(email: String): User = requireNotNull(
        tx.execute {
            val role = roleRepository.findByCode("admin") ?: error("admin role missing")
            userRepository.save(User(role, "Admin $email", email, passwordEncoder.encode("password123")))
        },
    )

    private fun cookieFor(user: User) = Cookie("access_token", jwtService.issueAccessToken(user))

    private fun png(name: String) = MockMultipartFile("files", name, "image/png", byteArrayOf(1, 2, 3, 4))

    private fun createAndSendPackage(): Long {
        val created = postJson("/api/v1/client/packages", mapOf("packageNumber" to "PKG-1", "description" to "modules"), clientCookie)
        val id = body(created)["data"]["id"].asLong()
        postEmpty("/api/v1/client/packages/$id/mark-sent", clientCookie)
        return id
    }

    private fun scanModule(packageId: Long, number: String): Long = body(
        postJson("/api/v1/technician/packages/$packageId/modules", mapOf("moduleNumber" to number, "problemTypeId" to 1), techCookie),
    )["data"]["id"].asLong()

    private fun repair(moduleId: Long) {
        postJson("/api/v1/technician/modules/$moduleId/repair", mapOf("decision" to "repaired", "price" to 10.0), techCookie)
    }

    private fun ship(packageId: Long) {
        postJson("/api/v1/technician/packages/$packageId/next-status", mapOf("returnTrackingLink" to "https://ret/1"), techCookie)
    }

    private fun statusAfterNext(packageId: Long): String =
        body(postJson("/api/v1/technician/packages/$packageId/next-status", emptyMap<String, Any>(), techCookie))["data"]["statusCode"].asText()

    private fun uploadImages(moduleId: Long, cookie: Cookie, vararg files: MockMultipartFile): MvcResult {
        val builder = multipart("/api/v1/technician/modules/$moduleId/images")
        files.forEach { builder.file(it) }
        return mockMvc.perform(builder.cookie(cookie)).andReturn()
    }

    private fun getReq(path: String, cookie: Cookie): MvcResult = mockMvc.perform(get(path).cookie(cookie)).andReturn()

    private fun postJson(path: String, payload: Any, cookie: Cookie): MvcResult =
        mockMvc.perform(
            post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)).cookie(cookie),
        ).andReturn()

    private fun postEmpty(path: String, cookie: Cookie): MvcResult = mockMvc.perform(post(path).cookie(cookie)).andReturn()

    private fun body(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)
}
