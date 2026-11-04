package com.wedora.web.dto;

import com.wedora.domain.Media;
import java.time.Instant;

public record MediaResponse(Long id, String url, String mediaType, String caption, Instant createdAt) {
    public static MediaResponse from(Media media) {
        return new MediaResponse(media.getId(), media.getUrl(), media.getMediaType(),
                media.getCaption(), media.getCreatedAt());
    }
}
