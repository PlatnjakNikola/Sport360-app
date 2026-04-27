package com.sport360.moduleservice.ratelimit

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/** In-memory token-bucket rate limiter (single instance). One bucket per key. */
@Component
class RateLimiter {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryConsume(key: String, capacity: Long, window: Duration): Boolean =
        buckets.computeIfAbsent(key) {
            Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, window).build())
                .build()
        }.tryConsume(1)

    /** Drops all buckets. Primarily useful to isolate tests. */
    fun clear() = buckets.clear()
}
