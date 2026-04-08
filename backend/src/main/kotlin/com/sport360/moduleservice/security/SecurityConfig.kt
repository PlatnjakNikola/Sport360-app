package com.sport360.moduleservice.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.sport360.moduleservice.common.ApiError
import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.ErrorCode
import com.sport360.moduleservice.config.AppProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AppProperties::class)
class SecurityConfig(
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(*PUBLIC_PATHS).permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(restAuthenticationEntryPoint())
                it.accessDeniedHandler(restAccessDeniedHandler())
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(props: AppProperties): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = props.cors.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Content-Type", "Authorization")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }

    private fun restAuthenticationEntryPoint() = AuthenticationEntryPoint { _, response, _ ->
        writeError(response, ErrorCode.UNAUTHORIZED, "Authentication required")
    }

    private fun restAccessDeniedHandler() = AccessDeniedHandler { _, response, _ ->
        writeError(response, ErrorCode.FORBIDDEN, "Access denied")
    }

    private fun writeError(response: jakarta.servlet.http.HttpServletResponse, code: ErrorCode, message: String) {
        response.status = code.status.value()
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(ApiResponse.failure(ApiError(code.name, message))))
    }

    private companion object {
        val PUBLIC_PATHS = arrayOf(
            "/api/v1/health",
            "/api/v1/public/**",
            "/api/v1/auth/login",
            "/api/v1/auth/mfa/verify",
            "/api/v1/auth/mfa/resend",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/logout",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/invite/*",
            "/api/v1/auth/accept-invite",
        )
    }
}
