package com.wedora.web;

import com.wedora.service.UserNotificationService;
import com.wedora.web.dto.NotificationCenterResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final UserNotificationService notifications;

    public NotificationController(UserNotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public NotificationCenterResponse list(@AuthenticationPrincipal String email) {
        return notifications.list(email);
    }

    @PutMapping("/{id}/read")
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal String email) {
        notifications.markRead(id, email);
    }

    @PutMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal String email) {
        notifications.markAllRead(email);
    }
}
