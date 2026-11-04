package com.wedora.service;

import com.wedora.domain.*;
import com.wedora.repository.*;
import com.wedora.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceAvailabilityTest {
    @Mock private BookingRepository bookings;
    @Mock private SupplierRepository suppliers;
    @Mock private UserRepository users;
    @Mock private ChecklistRepository checklist;
    @Mock private SupplierServiceOfferingRepository offerings;
    @Mock private SupplierAvailabilityBlockRepository availabilityBlocks;
    @Mock private EmailNotificationService notifications;
    @Mock private UserNotificationService userNotifications;

    private BookingService bookingService;
    private Booking booking;
    private SupplierServiceOffering offering;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookings, suppliers, users, checklist, offerings,
                availabilityBlocks, notifications, userNotifications);
        AppUser owner = new AppUser();
        owner.setEmail("supplier@example.test");
        AppUser customer = new AppUser();
        customer.setName("A Couple");
        customer.setEmail("couple@example.test");
        Category category = new Category();
        category.setSlug("venues");
        Supplier supplier = new Supplier();
        supplier.setId(3L);
        supplier.setOwner(owner);
        supplier.setCategory(category);
        supplier.setBusinessName("Garden Venue");
        offering = new SupplierServiceOffering();
        offering.setId(8L);
        offering.setSupplier(supplier);
        offering.setName("Garden");
        offering.setLocalizedContent(Map.of("en", Map.of("name", "Garden")));

        booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(customer);
        booking.setSupplier(supplier);
        booking.setServiceOffering(offering);
        booking.setEventDate(LocalDate.now().plusDays(120));
        booking.setStatus(BookingStatus.REQUESTED);

        when(bookings.findById(42L)).thenReturn(Optional.of(booking));
        when(offerings.findByIdForUpdate(8L)).thenReturn(Optional.of(offering));
        lenient().when(checklist.findByBookingId(anyLong())).thenReturn(List.of());
    }

    @Test
    void supplierAcceptanceReservesTheSelectedOptionDate() {
        when(availabilityBlocks.existsByServiceOfferingIdAndBlockedDate(8L, booking.getEventDate()))
                .thenReturn(false);

        var response = bookingService.accept(42L, "supplier@example.test");

        ArgumentCaptor<SupplierAvailabilityBlock> reservation =
                ArgumentCaptor.forClass(SupplierAvailabilityBlock.class);
        verify(availabilityBlocks).save(reservation.capture());
        assertThat(reservation.getValue().getServiceOffering()).isSameAs(offering);
        assertThat(reservation.getValue().getBlockedDate()).isEqualTo(booking.getEventDate());
        assertThat(reservation.getValue().getBooking()).isSameAs(booking);
        assertThat(response.status()).isEqualTo(BookingStatus.ACCEPTED);
        verify(notifications).supplierAccepted(booking);
        verify(userNotifications).notify(booking.getCustomer(), booking, "BOOKING_ACCEPTED", "Garden Venue");
    }

    @Test
    void supplierCannotAcceptWhenTheOptionDateHasBeenBlocked() {
        when(availabilityBlocks.existsByServiceOfferingIdAndBlockedDate(8L, booking.getEventDate()))
                .thenReturn(true);

        assertThatThrownBy(() -> bookingService.accept(42L, "supplier@example.test"))
                .isInstanceOf(ApiException.Conflict.class);

        verify(availabilityBlocks, never()).save(any());
    }

    @Test
    void decliningAnAcceptedRequestReleasesItsReservation() {
        booking.setStatus(BookingStatus.ACCEPTED);
        SupplierAvailabilityBlock reservation = new SupplierAvailabilityBlock();
        reservation.setBooking(booking);
        when(availabilityBlocks.findByServiceOfferingIdAndBlockedDate(8L, booking.getEventDate()))
                .thenReturn(Optional.of(reservation));

        var response = bookingService.decline(42L, "supplier@example.test");

        verify(availabilityBlocks).delete(reservation);
        assertThat(response.status()).isEqualTo(BookingStatus.DECLINED);
        verify(userNotifications).notify(booking.getCustomer(), booking, "BOOKING_DECLINED", "Garden Venue");
    }

    @Test
    void confirmingAQuotedOptionRequestReservesItsDate() {
        booking.setStatus(BookingStatus.QUOTED);
        when(availabilityBlocks.findByServiceOfferingIdAndBlockedDate(8L, booking.getEventDate()))
                .thenReturn(Optional.empty());

        var response = bookingService.confirm(42L, "couple@example.test");

        verify(availabilityBlocks).save(argThat(reservation ->
                reservation.getServiceOffering() == offering
                        && reservation.getBooking() == booking
                        && reservation.getBlockedDate().equals(booking.getEventDate())));
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
