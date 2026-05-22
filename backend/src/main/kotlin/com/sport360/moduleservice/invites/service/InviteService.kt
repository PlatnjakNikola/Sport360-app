package com.sport360.moduleservice.invites.service

import com.sport360.moduleservice.audit.service.AuditService
import com.sport360.moduleservice.clients.domain.Client
import com.sport360.moduleservice.clients.repository.ClientRepository
import com.sport360.moduleservice.common.ConflictException
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.ValidationException
import com.sport360.moduleservice.config.AppProperties
import com.sport360.moduleservice.email.EmailService
import com.sport360.moduleservice.invites.domain.ClientInviteToken
import com.sport360.moduleservice.invites.domain.TechnicianInviteToken
import com.sport360.moduleservice.invites.repository.ClientInviteTokenRepository
import com.sport360.moduleservice.invites.repository.TechnicianInviteTokenRepository
import com.sport360.moduleservice.invites.web.CreateClientInviteRequest
import com.sport360.moduleservice.invites.web.CreateTechnicianInviteRequest
import com.sport360.moduleservice.invites.web.InviteValidationResponse
import com.sport360.moduleservice.invites.web.PendingClientInviteResponse
import com.sport360.moduleservice.invites.web.PendingTechnicianInviteResponse
import com.sport360.moduleservice.notifications.service.NotificationService
import com.sport360.moduleservice.security.Tokens
import com.sport360.moduleservice.servicecenters.repository.ServiceCenterRepository
import com.sport360.moduleservice.technicians.domain.Technician
import com.sport360.moduleservice.technicians.repository.TechnicianRepository
import com.sport360.moduleservice.users.domain.User
import com.sport360.moduleservice.users.repository.RoleRepository
import com.sport360.moduleservice.users.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.OffsetDateTime

/** Technician + client invite lifecycle: create, validate, accept, resend, list pending. */
@Service
class InviteService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val serviceCenterRepository: ServiceCenterRepository,
    private val technicianRepository: TechnicianRepository,
    private val clientRepository: ClientRepository,
    private val technicianInviteTokenRepository: TechnicianInviteTokenRepository,
    private val clientInviteTokenRepository: ClientInviteTokenRepository,
    private val emailService: EmailService,
    private val auditService: AuditService,
    private val notificationService: NotificationService,
    appProperties: AppProperties,
) {

    private val inviteTtl: Duration = Duration.ofHours(48)
    private val frontendUrl = appProperties.frontendUrl.trimEnd('/')

    @Transactional
    fun createTechnicianInvite(adminId: Long, request: CreateTechnicianInviteRequest) {
        requireEmailAvailable(request.email)
        val serviceCenterId = request.serviceCenterId ?: throw ValidationException("Service center is required")
        val serviceCenter = serviceCenterRepository.findByIdAndActiveTrue(serviceCenterId)
            ?: throw ValidationException("Service center not found or inactive")
        val rawToken = Tokens.randomToken()
        val token = technicianInviteTokenRepository.save(
            TechnicianInviteToken(
                adminId, request.email, request.name, serviceCenter.id, request.phone,
                Tokens.sha256(rawToken), OffsetDateTime.now().plus(inviteTtl),
            ),
        )
        emailService.sendInvite(request.email, inviteLink(rawToken))
        auditService.record("technician_invite", token.id, "create", adminId)
    }

    @Transactional
    fun createClientInvite(adminId: Long, request: CreateClientInviteRequest) {
        requireEmailAvailable(request.email)
        val rawToken = Tokens.randomToken()
        val token = clientInviteTokenRepository.save(
            ClientInviteToken(
                adminId, request.email, request.contactName, request.companyName, request.contactPhone,
                request.address, Tokens.sha256(rawToken), OffsetDateTime.now().plus(inviteTtl),
            ),
        )
        emailService.sendInvite(request.email, inviteLink(rawToken))
        auditService.record("client_invite", token.id, "create", adminId)
    }

    @Transactional(readOnly = true)
    fun validate(rawToken: String): InviteValidationResponse {
        val hash = Tokens.sha256(rawToken)
        val now = OffsetDateTime.now()
        technicianInviteTokenRepository.findByTokenHash(hash)?.takeIf { it.isUsable(now) }?.let { token ->
            val serviceCenter = serviceCenterRepository.findById(token.serviceCenterId).orElse(null)
            return InviteValidationResponse(
                type = "technician",
                email = token.email,
                name = token.name,
                serviceCenterId = token.serviceCenterId,
                serviceCenterName = serviceCenter?.name,
            )
        }
        clientInviteTokenRepository.findByTokenHash(hash)?.takeIf { it.isUsable(now) }?.let { token ->
            return InviteValidationResponse(
                type = "client",
                email = token.email,
                name = token.contactName,
                companyName = token.companyName,
                contactPhone = token.contactPhone,
                address = token.address,
            )
        }
        throw NotFoundException("Invalid or expired invite")
    }

    @Transactional
    fun accept(rawToken: String, password: String) {
        val hash = Tokens.sha256(rawToken)
        val now = OffsetDateTime.now()
        technicianInviteTokenRepository.findByTokenHash(hash)?.takeIf { it.isUsable(now) }?.let { token ->
            acceptTechnician(token, password, now)
            return
        }
        clientInviteTokenRepository.findByTokenHash(hash)?.takeIf { it.isUsable(now) }?.let { token ->
            acceptClient(token, password, now)
            return
        }
        throw ValidationException("Invalid or expired invite")
    }

    @Transactional
    fun resendTechnicianInvite(adminId: Long, inviteId: Long) {
        val token = technicianInviteTokenRepository.findById(inviteId)
            .orElseThrow { NotFoundException("Invite not found") }
        if (token.usedAt != null) throw ConflictException("Invite already accepted")
        val rawToken = Tokens.randomToken()
        token.regenerate(Tokens.sha256(rawToken), OffsetDateTime.now().plus(inviteTtl))
        emailService.sendInvite(token.email, inviteLink(rawToken))
        auditService.record("technician_invite", token.id, "update", adminId)
    }

    @Transactional
    fun resendClientInvite(adminId: Long, inviteId: Long) {
        val token = clientInviteTokenRepository.findById(inviteId)
            .orElseThrow { NotFoundException("Invite not found") }
        if (token.usedAt != null) throw ConflictException("Invite already accepted")
        val rawToken = Tokens.randomToken()
        token.regenerate(Tokens.sha256(rawToken), OffsetDateTime.now().plus(inviteTtl))
        emailService.sendInvite(token.email, inviteLink(rawToken))
        auditService.record("client_invite", token.id, "update", adminId)
    }

    @Transactional(readOnly = true)
    fun listPendingTechnicianInvites(): List<PendingTechnicianInviteResponse> =
        technicianInviteTokenRepository.findAllByUsedAtIsNullOrderByCreatedAtDesc().map {
            PendingTechnicianInviteResponse(it.id, it.email, it.name, it.serviceCenterId, it.createdAt, it.expiresAt)
        }

    @Transactional(readOnly = true)
    fun listPendingClientInvites(): List<PendingClientInviteResponse> =
        clientInviteTokenRepository.findAllByUsedAtIsNullOrderByCreatedAtDesc().map {
            PendingClientInviteResponse(it.id, it.email, it.contactName, it.companyName, it.createdAt, it.expiresAt)
        }

    private fun acceptTechnician(token: TechnicianInviteToken, password: String, now: OffsetDateTime) {
        if (userRepository.existsByEmailIgnoreCase(token.email)) throw ConflictException("Email is already in use")
        val role = roleRepository.findByCode("technician") ?: error("Role 'technician' missing")
        val serviceCenter = serviceCenterRepository.findById(token.serviceCenterId)
            .orElseThrow { ValidationException("Service center no longer exists") }
        val user = userRepository.save(User(role, token.name, token.email, passwordEncoder.encode(password)))
        technicianRepository.save(Technician(user, serviceCenter, token.phone))
        token.markUsed(now)
        auditService.record("user", user.id, "create", user.id)
        notificationService.notifyAdmin(
            "technician_joined", "Technician joined", "${user.name} accepted the technician invite.", "user", user.id,
        )
    }

    private fun acceptClient(token: ClientInviteToken, password: String, now: OffsetDateTime) {
        if (userRepository.existsByEmailIgnoreCase(token.email)) throw ConflictException("Email is already in use")
        val role = roleRepository.findByCode("client") ?: error("Role 'client' missing")
        val user = userRepository.save(User(role, token.contactName, token.email, passwordEncoder.encode(password)))
        clientRepository.save(Client(user, token.companyName, token.contactPhone, token.address))
        token.markUsed(now)
        auditService.record("user", user.id, "create", user.id)
        notificationService.notifyAdmin(
            "client_joined", "Client joined", "${user.name} (${token.companyName}) accepted the client invite.", "user", user.id,
        )
    }

    private fun requireEmailAvailable(email: String) {
        val now = OffsetDateTime.now()
        val taken = userRepository.existsByEmailIgnoreCase(email) ||
            technicianInviteTokenRepository.existsByEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(email, now) ||
            clientInviteTokenRepository.existsByEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(email, now)
        if (taken) throw ConflictException("Email is already in use")
    }

    private fun inviteLink(rawToken: String) = "$frontendUrl/accept-invite/$rawToken"
}
