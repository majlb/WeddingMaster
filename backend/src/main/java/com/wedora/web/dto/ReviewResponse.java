package com.wedora.web.dto;

import com.wedora.domain.Review;
import java.time.Instant;

public record ReviewResponse(Long id, String authorName, int rating, String comment, Instant createdAt) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getAuthor().getName(), review.getRating(),
                review.getComment(), review.getCreatedAt());
    }
}
