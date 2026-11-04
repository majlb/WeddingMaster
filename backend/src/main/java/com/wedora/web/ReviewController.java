package com.wedora.web;

import com.wedora.service.ReviewService;
import com.wedora.web.dto.ReviewRequest;
import com.wedora.web.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/suppliers/{supplierId}/reviews")
    public List<ReviewResponse> list(@PathVariable Long supplierId) {
        return reviewService.list(supplierId);
    }

    @PostMapping("/bookings/{bookingId}/reviews")
    public ReviewResponse add(@PathVariable Long bookingId, @AuthenticationPrincipal String email,
                              @Valid @RequestBody ReviewRequest request) {
        return reviewService.add(bookingId, email, request);
    }
}
