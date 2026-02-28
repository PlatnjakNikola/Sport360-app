package com.sport360.moduleservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** Application configuration bound from the `app.*` namespace. */
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val frontendUrl: String = "http://localhost:5173",
    val defaultServiceCenterCode: String = "ZAGREB",
    val jwt: Jwt = Jwt(),
    val cookie: Cookie = Cookie(),
    val cors: Cors = Cors(),
    val mfa: Mfa = Mfa(),
    val passwordReset: PasswordReset = PasswordReset(),
    val bootstrap: Bootstrap = Bootstrap(),
    val image: Image = Image(),
) {
    data class Jwt(
        // Dev-only fallback (>= 32 bytes for HS256). Production must set JWT_SECRET.
        val secret: String = "dev-only-insecure-secret-change-me-please-0123456789",
        val accessTtl: Duration = Duration.ofMinutes(15),
        val refreshTtl: Duration = Duration.ofDays(7),
        val mfaTtl: Duration = Duration.ofMinutes(10),
    )

    data class Cookie(
        val secure: Boolean = false,
        val sameSite: String = "Strict",
    )

    data class Cors(
        val allowedOrigins: List<String> = listOf("http://localhost:5173", "http://localhost:5174"),
    )

    data class Mfa(
        val codeTtl: Duration = Duration.ofMinutes(10),
        val maxAttempts: Int = 5,
        val maxResends: Int = 3,
    )

    data class PasswordReset(
        val ttl: Duration = Duration.ofHours(1),
    )

    data class Bootstrap(
        val admin: Admin = Admin(),
    ) {
        data class Admin(
            val email: String = "",
            val password: String = "",
        )
    }

    data class Image(
        // Base directory for stored image files. In the cloud this MUST be a mounted persistent volume.
        val storagePath: String = "./data/images",
        val cleanupCron: String = "0 0 3 * * *",
        val retentionDays: Long = 30,
    )
}
