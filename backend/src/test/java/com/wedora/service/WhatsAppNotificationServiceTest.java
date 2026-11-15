package com.wedora.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotificationServiceTest {
    @Mock private RestTemplateBuilder restTemplateBuilder;
    @Mock private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    void sendsNotificationToTheSavedWhatsAppNumber() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));
        var service = new WhatsAppNotificationService(restTemplateBuilder, new ObjectMapper(), true,
                "AC123", "test-token", "whatsapp:+15550000000", "HX123", "HXINVITE");

        service.sendNotification("+15551234567", "BOOKING_REQUEST", "Alex",
                LocalDate.of(2027, 6, 12));

        ArgumentCaptor<HttpEntity> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(contains("/Accounts/AC123/"), request.capture(), eq(String.class));
        assertThat(request.getValue().getBody().toString())
                .contains("whatsapp:+15551234567")
                .contains("HX123")
                .contains("\"1\":\"Alex\"")
                .contains("\"2\":\"sent you a booking request\"")
                .contains("\"3\":\"for your wedding on June 12, 2027\"");
        assertThat(request.getValue().getHeaders().getFirst("Authorization")).startsWith("Basic ");
    }

    @Test
    void doesNotSendWhenDisabledOrPhoneNumberIsMissing() {
        var disabledService = new WhatsAppNotificationService(restTemplateBuilder, new ObjectMapper(), false,
                "", "", "", "", "");
        var enabledService = new WhatsAppNotificationService(restTemplateBuilder, new ObjectMapper(), true,
                "AC123", "test-token", "whatsapp:+15550000000", "HX123", "HXINVITE");

        disabledService.sendNotification("+15551234567", "NEW_MESSAGE", "Alex", null);
        enabledService.sendNotification(null, "NEW_MESSAGE", "Alex", null);

        verifyNoInteractions(restTemplate);
    }

    @Test
    void sendsImageTemplateAndPersonalizedMessageToTheGuest() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));
        var service = new WhatsAppNotificationService(restTemplateBuilder, new ObjectMapper(), true,
                "AC123", "test-token", "whatsapp:+15550000000", "HX123", "HXINVITE");

        service.sendInvitation("+15551234567", "Dear Sam, join our wedding!",
                "https://api.example.com/api/guest-invitations/media/00000000-0000-0000-0000-000000000001");

        ArgumentCaptor<HttpEntity> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(contains("/Accounts/AC123/"), request.capture(), eq(String.class));
        assertThat(request.getValue().getBody().toString())
                .contains("whatsapp:+15551234567")
                .contains("HXINVITE")
                .contains("\"1\":\"00000000-0000-0000-0000-000000000001\"")
                .contains("\"2\":\"Dear Sam, join our wedding!\"");
    }
}
