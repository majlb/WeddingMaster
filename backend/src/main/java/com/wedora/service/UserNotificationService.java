package com.wedora.service;

import com.wedora.domain.AppUser;
import com.wedora.domain.Booking;
import com.wedora.domain.UserNotification;
import com.wedora.repository.UserNotificationRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.NotificationCenterResponse;
import com.wedora.web.dto.UserNotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(UserNotificationService.class);
    private final UserNotificationRepository notifications;
    private final UserRepository users;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplate;

    public UserNotificationService(UserNotificationRepository notifications, UserRepository users,
                                   ObjectProvider<SimpMessagingTemplate> messagingTemplate) {
        this.notifications = notifications;
        this.users = users;
        this.messagingTemplate = messagingTemplate;
    }

    public void notify(AppUser recipient, Booking booking, String type, String actorName) {
        UserNotification notification = new UserNotification();
        notification.setRecipient(recipient);
        notification.setBooking(booking);
        notification.setType(type);
        notification.setActorName(actorName);
        UserNotification saved = notifications.save(notification);
        UserNotificationResponse response = UserNotificationResponse.from(saved);
        Runnable delivery = () -> {
            try {
                messagingTemplate.getObject().convertAndSendToUser(
                        recipient.getEmail(), "/queue/notifications", response);
            } catch (RuntimeException exception) {
                logger.error("In-app notification delivery failed for notification {}", saved.getId(), exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delivery.run();
                }
            });
        } else {
            delivery.run();
        }
    }

    @Transactional(readOnly = true)
    public NotificationCenterResponse list(String email) {
        AppUser recipient = userByEmail(email);
        List<UserNotificationResponse> result = notifications
                .findTop50ByRecipientIdOrderByCreatedAtDesc(recipient.getId()).stream()
                .map(UserNotificationResponse::from)
                .toList();
        return new NotificationCenterResponse(result,
                notifications.countByRecipientIdAndReadAtIsNull(recipient.getId()));
    }

    @Transactional
    public void markRead(Long notificationId, String email) {
        AppUser recipient = userByEmail(email);
        UserNotification notification = notifications.findByIdAndRecipientId(notificationId, recipient.getId())
                .orElseThrow(() -> new ApiException.NotFound("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
    }

    @Transactional
    public void markAllRead(String email) {
        AppUser recipient = userByEmail(email);
        List<UserNotification> unread = notifications.findByRecipientIdAndReadAtIsNull(recipient.getId());
        Instant now = Instant.now();
        unread.forEach(notification -> notification.setReadAt(now));
    }

    private AppUser userByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
    }
}
