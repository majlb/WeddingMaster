package com.wedora.web;

import com.wedora.service.ChatAttachmentService;
import com.wedora.web.dto.ChatAttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/bookings/{bookingId}/attachments")
public class ChatAttachmentController {
    private final ChatAttachmentService attachmentService;

    public ChatAttachmentController(ChatAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    public ChatAttachmentResponse upload(@PathVariable Long bookingId, @AuthenticationPrincipal String email,
                                         @RequestParam("file") MultipartFile file) {
        return attachmentService.upload(bookingId, email, file);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable Long bookingId, @PathVariable Long attachmentId,
                                             @AuthenticationPrincipal String email) {
        Resource resource = attachmentService.download(bookingId, attachmentId, email);
        var headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(resource.getFilename(), StandardCharsets.UTF_8).build());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
