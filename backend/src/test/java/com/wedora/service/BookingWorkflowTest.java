package com.wedora.service;

import com.wedora.domain.*;
import com.wedora.repository.*;
import com.wedora.web.ApiException;
import com.wedora.web.dto.QuoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingWorkflowTest {
    @Mock BookingRepository bookings;
    @Mock SupplierRepository suppliers;
    @Mock UserRepository users;
    @Mock ChecklistRepository checklist;
    @Mock SupplierServiceOfferingRepository offerings;
    @Mock SupplierAvailabilityBlockRepository availability;
    @Mock EmailNotificationService notifications;
    @Mock UserNotificationService userNotifications;
    BookingService service;
    Booking booking;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, suppliers, users, checklist, offerings, availability,
                notifications, userNotifications);
        AppUser customer = new AppUser();
        customer.setEmail("couple@example.test");
        customer.setName("Couple");
        AppUser owner = new AppUser();
        owner.setEmail("supplier@example.test");
        Supplier supplier = new Supplier();
        supplier.setOwner(owner);
        supplier.setBusinessName("Supplier");
        supplier.setTimeZone("Europe/Paris");
        booking = new Booking();
        booking.setId(1L);
        booking.setCustomer(customer);
        booking.setSupplier(supplier);
        booking.setEventDate(LocalDate.now().plusDays(30));
        booking.addHistory(BookingStatus.NEW_REQUEST, "CUSTOMER", "Hello");
        lenient().when(bookings.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        lenient().when(checklist.findByBookingId(1L)).thenReturn(List.of());
    }

    @Test
    void reviewNegotiationAcceptanceAndPaymentAreRecorded() {
        service.review(1L, "supplier@example.test", null);
        service.quote(1L, "supplier@example.test", new QuoteRequest(50000L, "EUR"));
        assertThatThrownBy(() -> service.review(1L, "couple@example.test", " "))
                .isInstanceOf(ApiException.class);
        service.review(1L, "couple@example.test", "Please revise");
        service.quote(1L, "supplier@example.test", new QuoteRequest(60000L, "EUR"));
        service.respondToQuote(1L, "couple@example.test");
        service.finalizeRequest(1L, "supplier@example.test");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.WAITING_PAYMENT);
        service.paymentSucceeded(1L);
        service.paymentSucceeded(1L);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTIVE);
        assertThat(booking.getHistory()).extracting(BookingHistory::getStatus).containsExactly(
                BookingStatus.NEW_REQUEST, BookingStatus.UNDER_REVIEW, BookingStatus.SENT_TO_COUPLE,
                BookingStatus.UNDER_REVIEW, BookingStatus.SENT_TO_COUPLE,
                BookingStatus.ACCEPTED_BY_COUPLE, BookingStatus.WAITING_PAYMENT, BookingStatus.ACTIVE);
        assertThat(booking.getHistory().get(3).getComment()).isEqualTo("Please revise");
    }

    @Test
    void declineRequiresCommentAndCorrectActor() {
        assertThatThrownBy(() -> service.declineRequest(1L, "supplier@example.test", " "))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.declineRequest(1L, "couple@example.test", "No"))
                .isInstanceOf(ApiException.Forbidden.class);
        service.declineRequest(1L, "supplier@example.test", "Unavailable");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.DECLINED_BY_SUPPLIER);
        assertThat(booking.getHistory().get(1).getComment()).isEqualTo("Unavailable");
    }

    @Test
    void quoteMustBeReviewedAndOnlyTheCoupleCanDeclineIt() {
        assertThatThrownBy(() -> service.quote(1L, "supplier@example.test", new QuoteRequest(500L, "EUR")))
                .isInstanceOf(ApiException.Conflict.class);
        service.review(1L, "supplier@example.test", null);
        service.quote(1L, "supplier@example.test", new QuoteRequest(500L, "EUR"));
        assertThatThrownBy(() -> service.declineRequest(1L, "supplier@example.test", "No"))
                .isInstanceOf(ApiException.Forbidden.class);
        service.declineRequest(1L, "couple@example.test", "Too expensive");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.DECLINED_BY_COUPLE);
        assertThat(booking.getHistory().get(3).getComment()).isEqualTo("Too expensive");
    }

    @Test
    void supplierCanDeclineAfterCoupleAccepts() {
        booking.setStatus(BookingStatus.ACCEPTED_BY_COUPLE);
        service.declineRequest(1L, "supplier@example.test", "Not available");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.DECLINED_BY_SUPPLIER);
    }

    @Test
    void supplierMustSetTimeZoneAndOnlyPaidPastBookingsClose() {
        booking.setStatus(BookingStatus.ACCEPTED_BY_COUPLE);
        booking.getSupplier().setTimeZone(null);
        assertThatThrownBy(() -> service.finalizeRequest(1L, "supplier@example.test"))
                .isInstanceOf(ApiException.Conflict.class);
        booking.getSupplier().setTimeZone("Pacific/Kiritimati");
        booking.setEventDate(LocalDate.now(java.time.ZoneId.of("Pacific/Kiritimati")).minusDays(1));
        booking.setStatus(BookingStatus.ACTIVE);
        when(bookings.findByStatus(BookingStatus.ACTIVE)).thenReturn(List.of(booking));
        service.closePastRequests();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CLOSED);
    }

    @Test
    void activeRequestStaysOpenUntilItsLocalEventDateHasPassed() {
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setEventDate(LocalDate.now(java.time.ZoneId.of("Europe/Paris")));
        when(bookings.findByStatus(BookingStatus.ACTIVE)).thenReturn(List.of(booking));
        service.closePastRequests();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTIVE);
    }

    @Test
    void legacyRequestedBookingCanStillBeQuoted() {
        booking.setStatus(BookingStatus.REQUESTED);
        service.quote(1L, "supplier@example.test", new QuoteRequest(500L, "EUR"));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.QUOTED);
    }
}
