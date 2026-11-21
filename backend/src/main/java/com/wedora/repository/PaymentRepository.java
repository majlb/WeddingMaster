package com.wedora.repository;

import com.wedora.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.stripePaymentIntentId = :intentId")
    Optional<Payment> findByStripePaymentIntentIdForUpdate(@Param("intentId") String intentId);
    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
