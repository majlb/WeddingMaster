package com.wedora.service;

import com.wedora.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitationMediaServiceTest {
    @TempDir
    Path directory;

    @Test
    void storesImagesAtTemporaryPublicUrlAndExpiresThemAfterOneDay() throws Exception {
        var service = new InvitationMediaService(directory.toString(), "https://api.example.com");
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        UUID id = service.store(png);

        assertThat(service.publicUrl(id))
                .isEqualTo("https://api.example.com/api/guest-invitations/media/" + id);
        assertThat(service.load(id).getContentAsByteArray()).containsExactly(png);

        Files.setLastModifiedTime(directory.resolve(id + ".png"),
                FileTime.from(Instant.now().minus(25, ChronoUnit.HOURS)));
        assertThatThrownBy(() -> service.load(id))
                .isInstanceOf(ApiException.NotFound.class);
    }

    @Test
    void requiresAnHttpsApiOriginForPublicMedia() throws Exception {
        var service = new InvitationMediaService(directory.toString(), "http://localhost:8080");

        assertThatThrownBy(service::validatePublicApiUrl)
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("PUBLIC_API_URL");
    }
}
