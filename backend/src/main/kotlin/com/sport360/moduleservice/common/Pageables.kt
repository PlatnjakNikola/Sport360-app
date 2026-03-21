package com.sport360.moduleservice.common

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

object Pageables {
    /** Builds a Pageable from 1-based page + limit query params, clamped to sane bounds. */
    fun of(page: Int, limit: Int): Pageable = PageRequest.of((page - 1).coerceAtLeast(0), limit.coerceIn(1, 100))
}
