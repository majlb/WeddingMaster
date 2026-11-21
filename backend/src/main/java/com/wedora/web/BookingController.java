package com.wedora.web;

import com.wedora.service.BookingService;
import com.wedora.web.dto.BookingRequest;
import com.wedora.web.dto.BookingResponse;
import com.wedora.web.dto.QuoteRequest;
import com.wedora.web.dto.BookingCommentRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<BookingResponse> list(@AuthenticationPrincipal String email) {
        return bookingService.list(email);
    }

    @PostMapping
    public BookingResponse request(@AuthenticationPrincipal String email, @Valid @RequestBody BookingRequest request) {
        return bookingService.request(email, request);
    }

    @PostMapping("/{id}/quote")
    public BookingResponse quote(@PathVariable Long id, @AuthenticationPrincipal String email,
                                 @Valid @RequestBody QuoteRequest request) {
        return bookingService.quote(id, email, request);
    }

    @PostMapping("/{id}/accept")
    public BookingResponse accept(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return bookingService.accept(id, email);
    }

    @PostMapping("/{id}/decline")
    public BookingResponse decline(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return bookingService.decline(id, email);
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return bookingService.confirm(id, email);
    }

    @PostMapping("/{id}/complete")
    public BookingResponse complete(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return bookingService.complete(id, email);
    }

    @PostMapping("/{id}/review")
    public BookingResponse review(@PathVariable Long id, @AuthenticationPrincipal String email,
                                  @RequestBody(required = false) BookingCommentRequest request) {
        return bookingService.review(id, email, request == null ? null : request.comment());
    }

    @PostMapping("/{id}/accept-quotation")
    public BookingResponse acceptQuotation(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return bookingService.respondToQuote(id, email);
    }

    @PostMapping("/{id}/finalize")
    public BookingResponse finalizeRequest(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return bookingService.finalizeRequest(id, email);
    }

    @PostMapping("/{id}/decline-request")
    public BookingResponse declineRequest(@PathVariable Long id, @AuthenticationPrincipal String email,
                                          @Valid @RequestBody BookingCommentRequest request) {
        return bookingService.declineRequest(id, email, request.comment());
    }
}
