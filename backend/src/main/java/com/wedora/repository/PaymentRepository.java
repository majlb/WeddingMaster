package com.wedora.repository;

import com.wedora.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
