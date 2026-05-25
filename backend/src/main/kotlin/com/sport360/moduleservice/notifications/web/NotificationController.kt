package com.sport360.moduleservice.notifications.web

import com.sport360.moduleservice.common.ApiResponse
import com.sport360.moduleservice.common.PageResponse
import com.sport360.moduleservice.common.Pageables
import com.sport360.moduleservice.notifications.service.NotificationService
import com.sport360.moduleservice.security.CurrentUserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Notifications for the authenticated user. The same handlers are exposed under each role's
 * path; every query is scoped to the current user id, so a user only ever sees their own.
 */
@RestController
@RequestMapping(path = ["/api/v1/admin/notifications", "/api/v1/technician/notifications", "/api/v1/client/notifications"])
class NotificationController(
    private val notificationService: NotificationService,
    private val currentUserService: CurrentUserService,
) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "false") unread: Boolean,
    ): ApiResponse<PageResponse<NotificationResponse>> =
        ApiResponse.ok(notificationService.list(currentUserService.currentUserId(), unread, Pageables.of(page, limit)))

    @GetMapping("/unread-count")
    fun unreadCount(): ApiResponse<UnreadCountResponse> =
        ApiResponse.ok(UnreadCountResponse(notificationService.unreadCount(currentUserService.currentUserId())))

    @PatchMapping("/{id}/read")
    fun markRead(@PathVariable id: Long): ApiResponse<Unit> {
        notificationService.markRead(currentUserService.currentUserId(), id)
        return ApiResponse.ok(Unit)
    }

    @PostMapping("/mark-all-read")
    fun markAllRead(): ApiResponse<Unit> {
        notificationService.markAllRead(currentUserService.currentUserId())
        return ApiResponse.ok(Unit)
    }
}
