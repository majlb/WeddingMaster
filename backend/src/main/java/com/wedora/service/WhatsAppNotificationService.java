package com.wedora.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedora.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
public class WhatsAppNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotificationService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String accountSid;
    private final String authToken;
    private final String sender;
    private final String contentSid;
    private final String invitationContentSid;

    public WhatsAppNotificationService(RestTemplateBuilder restTemplateBuilder,
                                       ObjectMapper objectMapper,
                                       @Value("${app.whatsapp.enabled:false}") boolean enabled,
                                       @Value("${app.whatsapp.twilio.account-sid:}") String accountSid,
                                       @Value("${app.whatsapp.twilio.auth-token:}") String authToken,
                                       @Value("${app.whatsapp.twilio.from:}") String sender,
                                       @Value("${app.whatsapp.twilio.content-sid:}") String contentSid,
                                       @Value("${app.whatsapp.twilio.invitation-content-sid:}") String invitationContentSid) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.sender = sender;
        this.contentSid = contentSid;
        this.invitationContentSid = invitationContentSid;
        if (enabled && (accountSid.isBlank() || authToken.isBlank() || sender.isBlank() || contentSid.isBlank())) {
            throw new IllegalStateException(
                    "Twilio account SID, auth token, WhatsApp sender, and approved content SID are required when WhatsApp is enabled");
        }
    }

    public void validateInvitation(String phoneNumber, String message) {
        if (!enabled || invitationContentSid.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Direct WhatsApp invitation delivery is not configured");
        }
        String normalizedNumber = phoneNumber == null ? "" : phoneNumber.replaceAll("[\\s()-]", "");
        if (!normalizedNumber.matches("\\+[1-9][0-9]{7,14}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The guest phone number must use international format");
        }
        if (message == null || message.isBlank() || message.length() > 1600) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invitation messages must contain 1 to 1600 characters");
        }
    }

    public void sendInvitation(String phoneNumber, String message, String mediaUrl) {
        validateInvitation(phoneNumber, message);
        String normalizedNumber = phoneNumber.replaceAll("[\\s()-]", "");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", whatsappAddress(normalizedNumber));
        form.add("From", whatsappAddress(sender));
        form.add("ContentSid", invitationContentSid);
        try {
            String mediaToken = mediaUrl.substring(mediaUrl.lastIndexOf('/') + 1);
            form.add("ContentVariables", objectMapper.writeValueAsString(Map.of(
                    "1", mediaToken,
                    "2", message)));
        } catch (JsonProcessingException exception) {
            logger.error("Could not prepare WhatsApp invitation content", exception);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "The invitation message could not be prepared");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(accountSid, authToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
        try {
            restTemplate.postForEntity(endpoint, request, String.class);
        } catch (RestClientException exception) {
            logger.error("WhatsApp invitation delivery failed", exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "WhatsApp could not accept this invitation. Check the recipient number and approved invitation template.");
        }
    }

    public void sendNotification(String phoneNumber, String type, String actorName, LocalDate eventDate) {
        if (!enabled || phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", whatsappAddress(phoneNumber));
        form.add("From", whatsappAddress(sender));
        form.add("ContentSid", contentSid);
        try {
            form.add("ContentVariables", objectMapper.writeValueAsString(contentVariables(type, actorName, eventDate)));
        } catch (JsonProcessingException exception) {
            logger.error("Could not prepare WhatsApp notification content for type {}", type, exception);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(accountSid, authToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        try {
            restTemplate.postForEntity(endpoint, request, String.class);
        } catch (RestClientException exception) {
            logger.error("WhatsApp notification delivery failed for notification type {}", type, exception);
        }
    }

    private String whatsappAddress(String number) {
        return number.startsWith("whatsapp:") ? number : "whatsapp:" + number;
    }

    private Map<String, String> contentVariables(String type, String actorName, LocalDate eventDate) {
        String action = switch (type) {
            case "BOOKING_REQUEST" -> "sent you a booking request";
            case "QUOTE_RECEIVED" -> "sent you a quote";
            case "BOOKING_ACCEPTED" -> "accepted your booking request";
            case "BOOKING_CONFIRMED" -> "confirmed your booking";
            case "BOOKING_DECLINED" -> "declined your booking request";
            case "BOOKING_COMPLETED" -> "marked your booking as completed";
            case "NEW_MESSAGE" -> "sent you a message";
            default -> "sent you an update";
        };
        String context = eventDate == null ? "about your wedding plans" : "for your wedding on " +
                eventDate.format(DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH));
        String actor = actorName == null || actorName.isBlank() ? "Someone" : actorName;
        return Map.of("1", actor, "2", action, "3", context);
    }
}
