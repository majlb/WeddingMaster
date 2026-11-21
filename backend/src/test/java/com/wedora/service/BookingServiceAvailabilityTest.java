package com.wedora.service;

import com.wedora.domain.*;
import com.wedora.repository.*;
import com.wedora.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        category.setName("Venues");
        Supplier supplier = new Supplier();
        supplier.setId(3L);
        supplier.setOwner(owner);
        supplier.setCategory(category);
        supplier.setBusinessName("Garden Venue");
        offering = new SupplierServiceOffering();
        offering.setId(8L);
        offering.setSupplier(supplier);
        offering.setCategory(category);
        offering.setName("Garden");
        offering.setLocalizedContent(Map.of("en", Map.of("name", "Garden")));

        booking = new Booking();
        booking.setId(42L);
        booking.setCustomer(customer);
        booking.setSupplier(supplier);
        booking.setServiceOffering(offering);
        booking.setEventDate(LocalDate.now().plusDays(120));
        booking.setStatus(BookingStatus.REQUESTED);

        lenient().when(bookings.findById(42L)).thenReturn(Optional.of(booking));
        lenient().when(checklist.findByBookingId(anyLong())).thenReturn(List.of());
    }

    @Test
    void supplierAcceptanceDoesNotBlockTheSelectedOptionDate() {
        var response = bookingService.accept(42L, "supplier@example.test");

        verifyNoInteractions(availabilityBlocks);
        assertThat(response.status()).isEqualTo(BookingStatus.ACCEPTED);
        verify(notifications).supplierAccepted(booking);
        verify(userNotifications).notify(booking.getCustomer(), booking, "BOOKING_ACCEPTED", "Garden Venue");
    }

    @Test
    void decliningAnAcceptedRequestKeepsManualBlocks() {
        booking.setStatus(BookingStatus.ACCEPTED);
        var response = bookingService.decline(42L, "supplier@example.test");

        verifyNoInteractions(availabilityBlocks);
        assertThat(response.status()).isEqualTo(BookingStatus.DECLINED);
        verify(userNotifications).notify(booking.getCustomer(), booking, "BOOKING_DECLINED", "Garden Venue");
    }

    @Test
    void confirmingAQuotedOptionRequestDoesNotBlockItsDate() {
        booking.setStatus(BookingStatus.QUOTED);
        var response = bookingService.confirm(42L, "couple@example.test");

        verifyNoInteractions(availabilityBlocks);
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void newRequestOnManuallyBlockedDateIsRejected() {
        AppUser customer = booking.getCustomer();
        customer.setRole(Role.CUSTOMER);
        when(users.findByEmailIgnoreCase("couple@example.test")).thenReturn(Optional.of(customer));
        when(suppliers.findByIdAndApprovedTrueAndActiveTrue(3L)).thenReturn(Optional.of(booking.getSupplier()));
        when(offerings.findByIdForUpdate(8L)).thenReturn(Optional.of(offering));
        when(availabilityBlocks.existsByServiceOfferingIdAndBlockedDate(8L, booking.getEventDate())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.request("couple@example.test",
                new com.wedora.web.dto.BookingRequest(3L, 8L, booking.getEventDate(), null, 1)))
                .isInstanceOf(ApiException.Conflict.class);
        verify(bookings, never()).save(any());
    }

    @Test
    void multipleRequestsCanUseAnUnblockedDate() {
        AppUser customer = booking.getCustomer();
        customer.setRole(Role.CUSTOMER);
        customer.setId(7L);
        when(users.findByEmailIgnoreCase("couple@example.test")).thenReturn(Optional.of(customer));
        when(suppliers.findByIdAndApprovedTrueAndActiveTrue(3L)).thenReturn(Optional.of(booking.getSupplier()));
        when(offerings.findByIdForUpdate(8L)).thenReturn(Optional.of(offering));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new com.wedora.web.dto.BookingRequest(3L, 8L, booking.getEventDate(), null, 1);

        assertThat(bookingService.request("couple@example.test", request).status()).isEqualTo(BookingStatus.NEW_REQUEST);
        assertThat(bookingService.request("couple@example.test", request).status()).isEqualTo(BookingStatus.NEW_REQUEST);
        verify(bookings, times(2)).save(any());
        verify(availabilityBlocks, never()).save(any());
    }

    @Test
    void rejectsQuantityOutsideServiceLimits() {
        AppUser customer = booking.getCustomer();
        customer.setRole(Role.CUSTOMER);
        offering.setMinimumAmount(2);
        offering.setMaximumAmount(5);
        when(users.findByEmailIgnoreCase("couple@example.test")).thenReturn(Optional.of(customer));
        when(suppliers.findByIdAndApprovedTrueAndActiveTrue(3L)).thenReturn(Optional.of(booking.getSupplier()));
        when(offerings.findByIdForUpdate(8L)).thenReturn(Optional.of(offering));
        assertThatThrownBy(() -> bookingService.request("couple@example.test",
                new com.wedora.web.dto.BookingRequest(3L, 8L, booking.getEventDate(), null, 6)))
                .isInstanceOf(ApiException.class);
        verify(bookings, never()).save(any());
    }

    @Test
    void generalRequestRequiresPositiveQuantity() {
        AppUser customer = booking.getCustomer();
        customer.setRole(Role.CUSTOMER);
        when(users.findByEmailIgnoreCase("couple@example.test")).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> bookingService.request("couple@example.test",
                new com.wedora.web.dto.BookingRequest(3L, null, booking.getEventDate(), null, null)))
                .isInstanceOf(ApiException.class);
        verify(bookings, never()).save(any());
    }
}
