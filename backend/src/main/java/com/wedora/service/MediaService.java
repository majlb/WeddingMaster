package com.wedora.service;

import com.wedora.domain.Media;
import com.wedora.domain.Supplier;
import com.wedora.repository.MediaRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.MediaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
public class MediaService {
    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp",
            "video/mp4", "mp4");
    private final MediaRepository mediaRepository;
    private final SupplierService supplierService;
    private final Path uploadDirectory;

    public MediaService(MediaRepository mediaRepository, SupplierService supplierService,
                        @Value("${app.uploads-dir}") String uploadsDirectory) {
        this.mediaRepository = mediaRepository;
        this.supplierService = supplierService;
        this.uploadDirectory = Paths.get(uploadsDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public MediaResponse upload(Long supplierId, String email, MultipartFile file, String caption) {
        return store(supplierId, email, file, caption, false);
    }

    @Transactional
    public MediaResponse uploadMainImage(Long supplierId, String email, MultipartFile file) {
        return store(supplierId, email, file, null, true);
    }

    @Transactional
    public String storeServiceImage(Long supplierId, String email, MultipartFile file) {
        supplierService.requireOwned(supplierId, email);
        if (file == null || file.isEmpty()) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose an image to upload");
        if (file.getSize() > 50L * 1024 * 1024) throw new ApiException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Files must be 50 MB or smaller");
        String contentType = file.getContentType();
        String extension = contentType == null ? null : SUPPORTED_TYPES.get(contentType.toLowerCase(Locale.ROOT));
        if (extension == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ApiException(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Service images must be JPEG, PNG, or WebP");
        }
        return saveUpload(file, extension);
    }

    private MediaResponse store(Long supplierId, String email, MultipartFile file, String caption, boolean mainImage) {
        Supplier supplier = supplierService.requireOwned(supplierId, email);
        if (file == null || file.isEmpty()) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a file to upload");
        if (file.getSize() > 50L * 1024 * 1024) throw new ApiException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Files must be 50 MB or smaller");
        String contentType = file.getContentType();
        String extension = contentType == null ? null : SUPPORTED_TYPES.get(contentType.toLowerCase(Locale.ROOT));
        if (extension == null) throw new ApiException(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, WebP, and MP4 files are supported");
        if (mainImage && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ApiException(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The main image must be a JPEG, PNG, or WebP image");
        }

        String url = saveUpload(file, extension);
        Media media = new Media();
        media.setSupplier(supplier);
        media.setUrl(url);
        media.setMediaType(contentType.toLowerCase(Locale.ROOT));
        media.setCaption(caption);
        Media savedMedia = mediaRepository.save(media);
        if (mainImage) supplier.setMainImageUrl(savedMedia.getUrl());
        return MediaResponse.from(savedMedia);
    }

    private String saveUpload(MultipartFile file, String extension) {
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDirectory);
            try (var input = file.getInputStream()) {
                Files.copy(input, uploadDirectory.resolve(filename));
            }
        } catch (IOException exception) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "The file could not be stored");
        }
        return "/uploads/" + filename;
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> listPublic(Long supplierId) {
        supplierService.requireApproved(supplierId);
        return mediaRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream()
                .map(MediaResponse::from).toList();
    }
}
