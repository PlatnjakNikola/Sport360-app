package com.sport360.moduleservice.common

import org.springframework.data.domain.Page

data class PageInfo(
    val page: Int,
    val limit: Int,
    val total: Long,
    val totalPages: Int,
)

/** Paginated list envelope payload: `{ items, pagination }` (1-based page). */
data class PageResponse<T>(
    val items: List<T>,
    val pagination: PageInfo,
) {
    companion object {
        fun <E, T> from(page: Page<E>, mapper: (E) -> T): PageResponse<T> =
            PageResponse(
                items = page.content.map(mapper),
                pagination = PageInfo(page.number + 1, page.size, page.totalElements, page.totalPages),
            )
    }
}
