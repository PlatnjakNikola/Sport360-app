package com.sport360.moduleservice.bootstrap

import com.sport360.moduleservice.config.AppProperties
import com.sport360.moduleservice.users.domain.User
import com.sport360.moduleservice.users.repository.RoleRepository
import com.sport360.moduleservice.users.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Creates the single admin account on first start from env vars, only if no admin exists.
 * The admin is never seeded in Flyway, keeping the password hash out of migrations.
 */
@Component
class AdminBootstrapRunner(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    props: AppProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val email = props.bootstrap.admin.email.trim()
    private val password = props.bootstrap.admin.password

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (userRepository.existsByRole_Code(ADMIN_ROLE)) {
            return
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No admin account exists and APP_BOOTSTRAP_ADMIN_EMAIL/PASSWORD are not set — skipping admin bootstrap.")
            return
        }
        val adminRole = roleRepository.findByCode(ADMIN_ROLE)
            ?: error("Role '$ADMIN_ROLE' is missing — check Flyway seed data.")
        userRepository.save(User(adminRole, "Administrator", email, passwordEncoder.encode(password)))
        log.info("Bootstrapped admin account: {}", email)
    }

    private companion object {
        const val ADMIN_ROLE = "admin"
    }
}
