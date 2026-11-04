package com.wedora.web;

import com.wedora.service.BookingService;
import com.wedora.web.dto.BookingRequest;
import com.wedora.web.dto.BookingResponse;
import com.wedora.web.dto.QuoteRequest;
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
}
