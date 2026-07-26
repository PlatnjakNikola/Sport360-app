package com.sport360.moduleservice

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Boots the full context against a real PostgreSQL 16, which runs Flyway V1.
 * Proves the migration applies and seeds the lookup tables, and that the three
 * read-model views are queryable.
 */
@SpringBootTest
@Testcontainers
class SchemaMigrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun `flyway seeds the lookup tables`() {
        assertThat(count("roles")).isEqualTo(3)
        assertThat(count("package_statuses")).isEqualTo(7)
        assertThat(count("module_statuses")).isEqualTo(3)
        assertThat(count("problem_types")).isEqualTo(13)
        assertThat(count("service_centers")).isEqualTo(1)
    }

    @Test
    fun `read-model views are queryable`() {
        jdbc.queryForList("SELECT * FROM v_package_summary")
        jdbc.queryForList("SELECT * FROM v_package_technician_breakdown")
        jdbc.queryForList("SELECT * FROM v_module_detail")
        jdbc.queryForList("SELECT * FROM v_admin_package_summary")
    }

    private fun count(table: String): Int =
        jdbc.queryForObject("SELECT count(*) FROM $table", Int::class.java) ?: 0
}
