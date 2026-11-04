package com.wedora.service;

import com.wedora.domain.*;
import com.wedora.repository.*;
import com.wedora.web.ApiException;
import com.wedora.web.dto.ReviewRequest;
import com.wedora.web.dto.ReviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final SupplierRepository suppliers;

    public ReviewService(ReviewRepository reviews, BookingRepository bookings,
                        UserRepository users, SupplierRepository suppliers) {
        this.reviews = reviews;
        this.bookings = bookings;
        this.users = users;
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> list(Long supplierId) {
        return reviews.findByBookingSupplierIdOrderByCreatedAtDesc(supplierId)
                .stream().map(ReviewResponse::from).toList();
    }

    @Transactional
    public ReviewResponse add(Long bookingId, String email, ReviewRequest request) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new ApiException.NotFound("Booking not found"));
        AppUser author = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        if (!booking.getCustomer().getId().equals(author.getId())) {
            throw new ApiException.Forbidden("Only the customer can review this booking");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ApiException.Conflict("Reviews are available after a booking is completed");
        }
        if (reviews.existsByBookingIdAndAuthorId(bookingId, author.getId())) {
            throw new ApiException.Conflict("You have already reviewed this booking");
        }
        Review review = new Review();
        review.setBooking(booking);
        review.setAuthor(author);
        review.setRating(request.rating());
        review.setComment(request.comment());
        reviews.save(review);

        Supplier supplier = booking.getSupplier();
        List<Review> supplierReviews = reviews.findByBookingSupplierIdOrderByCreatedAtDesc(supplier.getId());
        supplier.setReviewCount(supplierReviews.size());
        supplier.setRating(supplierReviews.stream().mapToInt(Review::getRating).average().orElse(0));
        return ReviewResponse.from(review);
    }
}
