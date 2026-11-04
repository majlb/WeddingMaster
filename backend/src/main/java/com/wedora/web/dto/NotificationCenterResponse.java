package com.wedora.web.dto;

import java.util.List;

public record NotificationCenterResponse(List<UserNotificationResponse> notifications, long unreadCount) {
}
