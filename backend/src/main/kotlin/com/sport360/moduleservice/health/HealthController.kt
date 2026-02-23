package com.sport360.moduleservice.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

/**
 * Liveness endpoint. Intentionally returns a plain object (not the standard
 * success envelope) — it is infrastructure, not part of the business API.
 */
@RestController
@RequestMapping("/api/v1/health")
class HealthController {

    @GetMapping
    fun health(): Map<String, String> = mapOf(
        "status" to "ok",
        "time" to OffsetDateTime.now().toString(),
    )
}
