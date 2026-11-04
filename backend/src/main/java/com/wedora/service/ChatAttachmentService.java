package com.wedora.service;

import com.wedora.domain.ChatAttachment;
import com.wedora.repository.ChatAttachmentRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.ChatAttachmentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatAttachmentService {
    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp",
            "video/mp4", "mp4", "application/pdf", "pdf");
    private final ChatAttachmentRepository attachments;
    private final UserRepository users;
    private final BookingService bookings;
    private final Path directory;

    public ChatAttachmentService(ChatAttachmentRepository attachments, UserRepository users,
                                 BookingService bookings,
                                 @Value("${app.chat-uploads-dir}") String directory) {
        this.attachments = attachments;
        this.users = users;
        this.bookings = bookings;
        this.directory = Paths.get(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public ChatAttachmentResponse upload(Long bookingId, String email, MultipartFile file) {
        var booking = bookings.requireParticipant(bookingId, email);
        if (file == null || file.isEmpty()) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a file to share");
        if (file.getSize() > 50L * 1024 * 1024) throw new ApiException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Files must be 50 MB or smaller");
        String contentType = file.getContentType();
        String extension = contentType == null ? null : SUPPORTED_TYPES.get(contentType.toLowerCase(Locale.ROOT));
        if (extension == null) throw new ApiException(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, WebP, MP4, and PDF files are supported");
        String key = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directory);
            try (var input = file.getInputStream()) {
                Files.copy(input, directory.resolve(key));
            }
        } catch (IOException exception) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "The attachment could not be stored");
        }
        var uploader = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        ChatAttachment attachment = new ChatAttachment();
        attachment.setBooking(booking);
        attachment.setUploader(uploader);
        attachment.setStorageKey(key);
        attachment.setFileName(safeFilename(file.getOriginalFilename(), extension));
        attachment.setContentType(contentType.toLowerCase(Locale.ROOT));
        attachments.save(attachment);
        return new ChatAttachmentResponse(attachment.getId(), attachment.getFileName(), attachment.getContentType());
    }

    @Transactional(readOnly = true)
    public Resource download(Long bookingId, Long attachmentId, String email) {
        bookings.requireParticipant(bookingId, email);
        ChatAttachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new ApiException.NotFound("Attachment not found"));
        if (!attachment.getBooking().getId().equals(bookingId)) {
            throw new ApiException.NotFound("Attachment not found");
        }
        Path file = directory.resolve(attachment.getStorageKey()).normalize();
        if (!file.getParent().equals(directory)) throw new ApiException.NotFound("Attachment not found");
        Resource resource = new FileSystemResource(file);
        if (!resource.exists() || !resource.isReadable()) throw new ApiException.NotFound("Attachment file is no longer available");
        return resource;
    }

    @Transactional(readOnly = true)
    public ChatAttachment requireForBooking(Long bookingId, Long attachmentId) {
        ChatAttachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new ApiException.NotFound("Attachment not found"));
        if (!attachment.getBooking().getId().equals(bookingId)) {
            throw new ApiException.Forbidden("Attachment belongs to a different conversation");
        }
        return attachment;
    }

    private String safeFilename(String originalFilename, String extension) {
        String name = originalFilename == null ? "shared-file" : originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isBlank()) name = "shared-file";
        if (name.length() > 170) name = name.substring(name.length() - 170);
        return name.contains(".") ? name : name + "." + extension;
    }
}
