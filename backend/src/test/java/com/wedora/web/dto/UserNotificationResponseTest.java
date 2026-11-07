package com.wedora.web.dto;

import com.wedora.domain.UserNotification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotificationResponseTest {
    @Test
    void mapsCollaborationNotificationWithoutBooking() {
        UserNotification notification = new UserNotification();
        notification.setId(5L);
        notification.setCollaborationId(12L);
        notification.setType("COLLABORATION_INVITATION");
        notification.setActorName("Beauty studio");

        UserNotificationResponse response = UserNotificationResponse.from(notification);

        assertThat(response.bookingId()).isNull();
        assertThat(response.collaborationId()).isEqualTo(12L);
        assertThat(response.eventDate()).isNull();
    }
}
