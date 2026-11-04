package com.wedora.web;

import com.wedora.service.PaymentService;
import com.wedora.web.dto.InvoiceResponse;
import com.wedora.web.dto.PaymentResponse;
import com.wedora.web.dto.RefundResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/bookings/{bookingId}/payments")
    public PaymentResponse createPayment(@PathVariable Long bookingId, @AuthenticationPrincipal String email) {
        return paymentService.createIntent(bookingId, email);
    }

    @GetMapping("/api/payments")
    public List<PaymentResponse> history(@AuthenticationPrincipal String email) {
        return paymentService.history(email);
    }

    @PostMapping("/api/admin/payments/{paymentId}/refund")
    public RefundResponse refund(@PathVariable Long paymentId) {
        return paymentService.refund(paymentId);
    }

    @PostMapping("/api/payments/webhook")
    public void webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        paymentService.processWebhook(payload, signature);
    }

    @GetMapping("/api/bookings/{bookingId}/invoice")
    public InvoiceResponse invoice(@PathVariable Long bookingId, @AuthenticationPrincipal String email) {
        return paymentService.invoice(bookingId, email);
    }
}
