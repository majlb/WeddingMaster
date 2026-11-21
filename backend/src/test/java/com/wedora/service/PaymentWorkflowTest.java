package com.wedora.service;

import com.stripe.Stripe;
import com.wedora.domain.Booking;
import com.wedora.domain.Payment;
import com.wedora.domain.PaymentStatus;
import com.wedora.repository.ChecklistRepository;
import com.wedora.repository.PaymentRepository;
import com.wedora.repository.UserRepository;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentWorkflowTest {
    @Test
    void signedSuccessActivatesWaitingRequestAndDuplicateIsIgnored() throws Exception {
        PaymentRepository payments = mock(PaymentRepository.class);
        BookingService bookings = mock(BookingService.class);
        ChecklistRepository checklist = mock(ChecklistRepository.class);
        PaymentService service = new PaymentService(payments, mock(UserRepository.class), bookings,
                checklist, "", "whsec_test");
        Booking booking = new Booking();
        booking.setId(42L);
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByStripePaymentIntentIdForUpdate("pi_test")).thenReturn(Optional.of(payment));
        when(checklist.findByBookingId(42L)).thenReturn(List.of());
        String payload = "{\"id\":\"evt_test\",\"object\":\"event\",\"api_version\":\"" + Stripe.API_VERSION
                + "\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_test\","
                + "\"object\":\"payment_intent\",\"status\":\"succeeded\"}}}";
        long timestamp = System.currentTimeMillis() / 1000;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String digest = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        String signature = "t=" + timestamp + ",v1=" + digest;

        service.processWebhook(payload, signature);
        service.processWebhook(payload, signature);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(bookings, times(1)).paymentSucceeded(42L);
    }
}
