package com.wedora.service;

import com.wedora.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class InvitationMediaService {
    private static final Logger logger = LoggerFactory.getLogger(InvitationMediaService.class);
    private static final Duration MEDIA_RETENTION = Duration.ofHours(24);
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private final Path directory;
    private final String publicApiUrl;

    public InvitationMediaService(@Value("${app.invitation-uploads-dir}") String directory,
                                  @Value("${app.public-api-url:}") String publicApiUrl) throws IOException {
        this.directory = Paths.get(directory).toAbsolutePath().normalize();
        this.publicApiUrl = publicApiUrl;
        Files.createDirectories(this.directory);
    }

    public UUID store(byte[] image) {
        if (image == null || image.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose an invitation card to send");
        }
        if (image.length > MAX_IMAGE_SIZE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Invitation cards must be 5 MB or smaller");
        }
        if (!isPng(image)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invitation cards must be PNG images");
        }

        UUID id = UUID.randomUUID();
        try {
            Files.write(pathFor(id), image);
        } catch (IOException exception) {
            logger.error("Could not store invitation image {}", id, exception);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The invitation card could not be stored");
        }
        return id;
    }

    public Resource load(UUID id) {
        Path image = pathFor(id);
        try {
            if (!Files.isRegularFile(image)
                    || Files.getLastModifiedTime(image).toInstant().isBefore(Instant.now().minus(MEDIA_RETENTION))) {
                throw new ApiException.NotFound("Invitation image not found");
            }
            return new FileSystemResource(image);
        } catch (IOException exception) {
            throw new ApiException.NotFound("Invitation image not found");
        }
    }

    public String publicUrl(UUID id) {
        URI baseUri = publicApiOrigin();
        return baseUri.toString().replaceAll("/+$", "")
                + "/api/guest-invitations/media/" + id;
    }

    public void validatePublicApiUrl() {
        publicApiOrigin();
    }

    private URI publicApiOrigin() {
        URI baseUri;
        try {
            baseUri = URI.create(publicApiUrl);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Direct WhatsApp delivery requires a configured public API URL");
        }
        if (!"https".equalsIgnoreCase(baseUri.getScheme()) || baseUri.getHost() == null
                || baseUri.getUserInfo() != null
                || (baseUri.getRawPath() != null && !baseUri.getRawPath().isEmpty()
                && !"/".equals(baseUri.getRawPath()))
                || baseUri.getRawQuery() != null || baseUri.getRawFragment() != null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Direct WhatsApp delivery requires PUBLIC_API_URL to be the public HTTPS API origin");
        }
        return baseUri;
    }

    @Scheduled(fixedDelay = 60 * 60 * 1000L)
    public void deleteExpiredImages() {
        Instant cutoff = Instant.now().minus(MEDIA_RETENTION);
        try (var files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().matches("[0-9a-fA-F-]{36}\\.png"))
                    .forEach(path -> deleteIfExpired(path, cutoff));
        } catch (IOException exception) {
            logger.error("Could not clean up expired invitation images", exception);
        }
    }

    private void deleteIfExpired(Path path, Instant cutoff) {
        try {
            FileTime modifiedAt = Files.getLastModifiedTime(path);
            if (modifiedAt.toInstant().isBefore(cutoff)) Files.deleteIfExists(path);
        } catch (IOException exception) {
            logger.warn("Could not delete expired invitation image {}", path.getFileName(), exception);
        }
    }

    private Path pathFor(UUID id) {
        return directory.resolve(id + ".png");
    }

    private boolean isPng(byte[] image) {
        return image.length >= 8
                && image[0] == (byte) 0x89
                && image[1] == 0x50
                && image[2] == 0x4e
                && image[3] == 0x47
                && image[4] == 0x0d
                && image[5] == 0x0a
                && image[6] == 0x1a
                && image[7] == 0x0a;
    }
}
