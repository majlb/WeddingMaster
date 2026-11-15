package com.wedora.web;

import com.wedora.domain.AppUser;
import com.wedora.domain.Guest;
import com.wedora.repository.GuestRepository;
import com.wedora.repository.UserRepository;
import com.wedora.service.InvitationMediaService;
import com.wedora.service.WhatsAppNotificationService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
public class GuestInvitationController {
    private final GuestRepository guestRepository;
    private final UserRepository userRepository;
    private final InvitationMediaService invitationMedia;
    private final WhatsAppNotificationService whatsAppNotifications;

    public GuestInvitationController(GuestRepository guestRepository, UserRepository userRepository,
                                     InvitationMediaService invitationMedia,
                                     WhatsAppNotificationService whatsAppNotifications) {
        this.guestRepository = guestRepository;
        this.userRepository = userRepository;
        this.invitationMedia = invitationMedia;
        this.whatsAppNotifications = whatsAppNotifications;
    }

    @PostMapping(value = "/api/guests/{guestId}/invitation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> send(@PathVariable Long guestId,
                                    @AuthenticationPrincipal String email,
                                    @RequestPart("card") MultipartFile card,
                                    @RequestPart("message") String message) {
        if (message == null || message.isBlank() || message.length() > 1600) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invitation messages must contain 1 to 1600 characters");
        }
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));
        Guest guest = guestRepository.findByIdAndWeddingCoupleId(guestId, user.getId())
                .orElseThrow(() -> new ApiException.NotFound("Guest not found"));
        if (guest.getPhone() == null || guest.getPhone().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Add a phone number to this guest before sending an invitation");
        }
        if (card.getSize() > 5L * 1024 * 1024) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Invitation cards must be 5 MB or smaller");
        }

        String contentType = card.getContentType();
        if (!MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Invitation cards must be PNG images");
        }
        whatsAppNotifications.validateInvitation(guest.getPhone(), message);
        invitationMedia.validatePublicApiUrl();
        try {
            UUID mediaId = invitationMedia.store(card.getBytes());
            String mediaUrl = invitationMedia.publicUrl(mediaId);
            whatsAppNotifications.sendInvitation(guest.getPhone(), message, mediaUrl);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The invitation card could not be read");
        }
        return Map.of("status", "queued");
    }

    @GetMapping(value = "/api/guest-invitations/media/{mediaId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> media(@PathVariable UUID mediaId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.IMAGE_PNG)
                .body(invitationMedia.load(mediaId));
    }
}
