package com.wedora.service;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.wedora.domain.*;
import com.wedora.repository.ChecklistRepository;
import com.wedora.repository.PaymentRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.InvoiceResponse;
import com.wedora.web.dto.PaymentResponse;
import com.wedora.web.dto.RefundResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final UserRepository users;
    private final BookingService bookings;
    private final ChecklistRepository checklist;
    private final String stripeSecret;
    private final String webhookSecret;

    public PaymentService(PaymentRepository payments, UserRepository users, BookingService bookings,
                          ChecklistRepository checklist,
                          @Value("${app.stripe.secret-key}") String stripeSecret,
                          @Value("${app.stripe.webhook-secret}") String webhookSecret) {
        this.payments = payments;
        this.users = users;
        this.bookings = bookings;
        this.checklist = checklist;
        this.stripeSecret = stripeSecret;
        this.webhookSecret = webhookSecret;
        if (!stripeSecret.isBlank()) Stripe.apiKey = stripeSecret;
    }

    @Transactional
    public PaymentResponse createIntent(Long bookingId, String email) {
        requireStripeConfigured();
        Booking booking = bookings.requireParticipant(bookingId, email);
        if (!booking.getCustomer().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException.Forbidden("Only the customer can pay for this booking");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED || booking.getQuotedAmountCents() == null) {
            throw new ApiException.Conflict("A confirmed booking with an agreed quote is required");
        }
        Payment previous = payments.findFirstByBookingIdOrderByCreatedAtDesc(bookingId).orElse(null);
        if (previous != null && previous.getStatus() != PaymentStatus.FAILED && previous.getStatus() != PaymentStatus.REFUNDED) {
            throw new ApiException.Conflict("This booking already has a payment in progress or completed");
        }
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(booking.getQuotedAmountCents())
                .setCurrency(booking.getCurrency().toLowerCase(Locale.ROOT))
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                .putMetadata("bookingId", bookingId.toString())
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params);
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setAmountCents(booking.getQuotedAmountCents());
            payment.setCurrency(booking.getCurrency());
            payment.setStripePaymentIntentId(intent.getId());
            payment.setStatus(PaymentStatus.PENDING);
            payments.save(payment);
            return PaymentResponse.from(payment, intent.getClientSecret());
        } catch (StripeException exception) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Payment provider could not create a payment intent");
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> history(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
        return payments.findByBookingCustomerIdOrderByCreatedAtDesc(user.getId())
                .stream().map(payment -> PaymentResponse.from(payment, null)).toList();
    }

    @Transactional
    public RefundResponse refund(Long paymentId) {
        requireStripeConfigured();
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ApiException.NotFound("Payment not found"));
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new ApiException.Conflict("Only successful payments can be refunded");
        }
        try {
            Refund refund = Refund.create(RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .build());
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            return new RefundResponse(refund.getId(), refund.getStatus());
        } catch (StripeException exception) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Payment provider could not process the refund");
        }
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        if (webhookSecret.isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Payment webhook is not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException | JsonSyntaxException exception) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid payment webhook signature or payload");
        }
        String type = event.getType();
        if (!type.equals("payment_intent.succeeded") && !type.equals("payment_intent.payment_failed")
                && !type.equals("charge.refunded")) return;
        StripeObject object = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Payment webhook payload could not be decoded"));
        String intentId;
        PaymentStatus status;
        if (object instanceof PaymentIntent intent) {
            intentId = intent.getId();
            status = type.equals("payment_intent.succeeded") ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        } else if (object instanceof Charge charge && type.equals("charge.refunded")) {
            intentId = charge.getPaymentIntent();
            status = charge.getRefunded() ? PaymentStatus.REFUNDED : PaymentStatus.REFUND_PENDING;
        } else {
            return;
        }
        payments.findByStripePaymentIntentId(intentId).ifPresent(payment -> {
            payment.setStatus(status);
            checklist.findByBookingId(payment.getBooking().getId()).forEach(item ->
                    item.setPaymentStatus(status == PaymentStatus.SUCCEEDED ? "Paid" :
                            status == PaymentStatus.REFUNDED ? "Refunded" :
                                    status == PaymentStatus.FAILED ? "Payment failed" : "Refund processing"));
        });
    }

    @Transactional(readOnly = true)
    public InvoiceResponse invoice(Long bookingId, String email) {
        Booking booking = bookings.requireParticipant(bookingId, email);
        Payment payment = payments.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .orElseThrow(() -> new ApiException.NotFound("A paid invoice is not available for this booking"));
        AppUser customer = users.findByEmailIgnoreCase(booking.getCustomer().getEmail())
                .orElseThrow(() -> new ApiException.NotFound("Customer account not found"));
        return new InvoiceResponse("WD-" + payment.getId(),
                Instant.now(), booking.getId(), customer.getName(), booking.getSupplier().getBusinessName(),
                booking.getEventDate().toString(), payment.getAmountCents(), payment.getCurrency(),
                payment.getStripePaymentIntentId());
    }

    private void requireStripeConfigured() {
        if (stripeSecret.isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Online payments are not configured. Set STRIPE_SECRET_KEY first.");
        }
    }
}
