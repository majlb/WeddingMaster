package com.wedora.web;

import com.wedora.service.MediaService;
import com.wedora.web.dto.MediaResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers/{supplierId}/media")
public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public List<MediaResponse> list(@PathVariable Long supplierId) {
        return mediaService.listPublic(supplierId);
    }

    @PostMapping
    public MediaResponse upload(@PathVariable Long supplierId, @AuthenticationPrincipal String email,
                                @RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) String caption) {
        return mediaService.upload(supplierId, email, file, caption);
    }

    @PostMapping("/main-image")
    public MediaResponse uploadMainImage(@PathVariable Long supplierId, @AuthenticationPrincipal String email,
                                         @RequestParam("file") MultipartFile file) {
        return mediaService.uploadMainImage(supplierId, email, file);
    }
}
