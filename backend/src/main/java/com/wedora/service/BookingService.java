package com.wedora.service;

import com.wedora.domain.*;
import com.wedora.repository.*;
import com.wedora.web.ApiException;
import com.wedora.web.dto.BookingRequest;
import com.wedora.web.dto.BookingResponse;
import com.wedora.web.dto.QuoteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final SupplierRepository suppliers;
    private final UserRepository users;
    private final ChecklistRepository checklist;
    private final SupplierServiceOfferingRepository offerings;
    private final SupplierAvailabilityBlockRepository availabilityBlocks;
    private final EmailNotificationService notifications;
    private final UserNotificationService userNotifications;

    public BookingService(BookingRepository bookings, SupplierRepository suppliers,
                         UserRepository users, ChecklistRepository checklist,
                         SupplierServiceOfferingRepository offerings,
                         SupplierAvailabilityBlockRepository availabilityBlocks,
                         EmailNotificationService notifications, UserNotificationService userNotifications) {
        this.bookings = bookings;
        this.suppliers = suppliers;
        this.users = users;
        this.checklist = checklist;
        this.offerings = offerings;
        this.availabilityBlocks = availabilityBlocks;
        this.notifications = notifications;
        this.userNotifications = userNotifications;
    }

    @Transactional
    public BookingResponse request(String email, BookingRequest request) {
        AppUser customer = userByEmail(email);
        if (customer.getRole() != Role.CUSTOMER) throw new ApiException.Forbidden("Only customer accounts can request bookings");
        Supplier supplier = suppliers.findByIdAndApprovedTrueAndActiveTrue(request.supplierId())
                .orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        SupplierServiceOffering offering = null;
        if (request.serviceOfferingId() != null) {
            offering = offerings.findByIdForUpdate(request.serviceOfferingId())
                    .orElseThrow(() -> new ApiException.NotFound("Supplier service not found"));
            if (!offering.getSupplier().getId().equals(supplier.getId())) {
                throw new ApiException.NotFound("Supplier service not found");
            }
            if (availabilityBlocks.existsByServiceOfferingIdAndBlockedDate(
                    offering.getId(), request.eventDate())) {
                throw new ApiException.Conflict("This service is not available on the selected date");
            }
        }
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSupplier(supplier);
        booking.setServiceOffering(offering);
        booking.setEventDate(request.eventDate());
        booking.setNotes(request.notes());
        booking.setCurrency(supplier.getCurrency());
        booking = bookings.save(booking);

        var checklistCategory = offering == null ? supplier.getCategory() : offering.getCategory();
        if (checklistCategory == null) {
            checklistCategory = supplier.getCategories().stream().findFirst()
                    .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                            "Supplier has not selected a service category"));
        }
        var selectedCategory = checklistCategory;
        ChecklistItem item = checklist.findByCustomerIdOrderByCreatedAtAsc(customer.getId()).stream()
                .filter(candidate -> !candidate.isCompleted() && candidate.getBooking() == null
                        && selectedCategory.getSlug().equals(candidate.getCategorySlug()))
                .findFirst().orElseGet(() -> {
                    ChecklistItem customItem = new ChecklistItem();
                    customItem.setCustomer(customer);
                    customItem.setTitle("Book " + selectedCategory.getName().toLowerCase(Locale.ROOT));
                    customItem.setCategorySlug(selectedCategory.getSlug());
                    return customItem;
                });
        item.setBooking(booking);
        item.setNotes(request.notes());
        item.setContractDetails("Booking request sent");
        item.setPaymentStatus("Awaiting supplier quote");
        checklist.save(item);
        notifications.bookingRequested(booking);
        userNotifications.notify(supplier.getOwner(), booking, "BOOKING_REQUEST", customer.getName());
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> list(String email) {
        AppUser user = userByEmail(email);
        List<Booking> results = user.getRole() == Role.SUPPLIER
                ? bookings.findBySupplierOwnerIdOrderByCreatedAtDesc(user.getId())
                : bookings.findByCustomerIdOrderByCreatedAtDesc(user.getId());
        return results.stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse quote(Long bookingId, String email, QuoteRequest request) {
        Booking booking = bookingById(bookingId);
        ensureSupplierOwner(booking, email);
        if (booking.getStatus() != BookingStatus.REQUESTED && booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new ApiException.Conflict("Only requested or accepted bookings can be quoted");
        }
        booking.setQuotedAmountCents(request.amountCents());
        if (request.currency() != null) {
            if (!request.currency().matches("[A-Za-z]{3}")) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Currency must be a three-letter ISO code");
            booking.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        }
        booking.setStatus(BookingStatus.QUOTED);
        updateChecklist(booking, "Quote received", "Awaiting your confirmation");
        notifications.quoteReceived(booking);
        userNotifications.notify(booking.getCustomer(), booking, "QUOTE_RECEIVED",
                booking.getSupplier().getBusinessName());
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse accept(Long bookingId, String email) {
        Booking booking = bookingById(bookingId);
        ensureSupplierOwner(booking, email);
        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new ApiException.Conflict("Only requested bookings can be accepted");
        }
        if (booking.getServiceOffering() != null) {
            SupplierServiceOffering offering = offerings.findByIdForUpdate(booking.getServiceOffering().getId())
                    .orElseThrow(() -> new ApiException.Conflict("This supplier service is no longer available"));
            if (availabilityBlocks.existsByServiceOfferingIdAndBlockedDate(
                    offering.getId(), booking.getEventDate())) {
                throw new ApiException.Conflict("This service has already been booked or blocked for that date");
            }
            SupplierAvailabilityBlock reservation = new SupplierAvailabilityBlock();
            reservation.setServiceOffering(offering);
            reservation.setBlockedDate(booking.getEventDate());
            reservation.setBooking(booking);
            availabilityBlocks.save(reservation);
        }
        booking.setStatus(BookingStatus.ACCEPTED);
        updateChecklist(booking, "Supplier accepted", "Awaiting supplier quote");
        notifications.supplierAccepted(booking);
        userNotifications.notify(booking.getCustomer(), booking, "BOOKING_ACCEPTED",
                booking.getSupplier().getBusinessName());
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse confirm(Long bookingId, String email) {
        Booking booking = bookingById(bookingId);
        if (!booking.getCustomer().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException.Forbidden("Only the customer can confirm this quote");
        }
        if (booking.getStatus() != BookingStatus.QUOTED) {
            throw new ApiException.Conflict("Only a quoted booking can be confirmed");
        }
        if (booking.getServiceOffering() != null) {
            SupplierServiceOffering offering = offerings.findByIdForUpdate(booking.getServiceOffering().getId())
                    .orElseThrow(() -> new ApiException.Conflict("This supplier service is no longer available"));
            var reservation = availabilityBlocks.findByServiceOfferingIdAndBlockedDate(
                    offering.getId(), booking.getEventDate());
            if (reservation.isPresent()) {
                if (reservation.get().getBooking() == null
                        || !reservation.get().getBooking().getId().equals(booking.getId())) {
                    throw new ApiException.Conflict("This service has already been booked or blocked for that date");
                }
            } else {
                SupplierAvailabilityBlock newReservation = new SupplierAvailabilityBlock();
                newReservation.setServiceOffering(offering);
                newReservation.setBlockedDate(booking.getEventDate());
                newReservation.setBooking(booking);
                availabilityBlocks.save(newReservation);
            }
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        updateChecklist(booking, "Booking confirmed", "Payment due");
        notifications.bookingConfirmed(booking);
        userNotifications.notify(booking.getSupplier().getOwner(), booking, "BOOKING_CONFIRMED",
                booking.getCustomer().getName());
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse decline(Long bookingId, String email) {
        Booking booking = bookingById(bookingId);
        ensureSupplierOwner(booking, email);
        if (booking.getStatus() != BookingStatus.REQUESTED && booking.getStatus() != BookingStatus.ACCEPTED
                && booking.getStatus() != BookingStatus.QUOTED) {
            throw new ApiException.Conflict("This booking can no longer be declined");
        }
        if (booking.getServiceOffering() != null) {
            offerings.findByIdForUpdate(booking.getServiceOffering().getId())
                    .orElseThrow(() -> new ApiException.Conflict("This supplier service is no longer available"));
            availabilityBlocks.findByServiceOfferingIdAndBlockedDate(
                    booking.getServiceOffering().getId(), booking.getEventDate())
                    .filter(block -> block.getBooking() != null
                            && block.getBooking().getId().equals(booking.getId()))
                    .ifPresent(availabilityBlocks::delete);
        }
        booking.setStatus(BookingStatus.DECLINED);
        updateChecklist(booking, "Supplier declined", "No payment due");
        userNotifications.notify(booking.getCustomer(), booking, "BOOKING_DECLINED",
                booking.getSupplier().getBusinessName());
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse complete(Long bookingId, String email) {
        Booking booking = bookingById(bookingId);
        ensureSupplierOwner(booking, email);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ApiException.Conflict("Only confirmed bookings can be completed");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        userNotifications.notify(booking.getCustomer(), booking, "BOOKING_COMPLETED",
                booking.getSupplier().getBusinessName());
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public Booking requireParticipant(Long bookingId, String email) {
        Booking booking = bookingById(bookingId);
        boolean customer = booking.getCustomer().getEmail().equalsIgnoreCase(email);
        boolean supplier = booking.getSupplier().getOwner().getEmail().equalsIgnoreCase(email);
        if (!customer && !supplier) throw new ApiException.Forbidden("You are not part of this booking");
        return booking;
    }

    private void updateChecklist(Booking booking, String contract, String payment) {
        checklist.findByBookingId(booking.getId()).forEach(item -> {
            item.setCompleted(booking.getStatus() == BookingStatus.CONFIRMED);
            item.setContractDetails(contract);
            item.setPaymentStatus(payment);
        });
    }

    private void ensureSupplierOwner(Booking booking, String email) {
        if (!booking.getSupplier().getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException.Forbidden("Only this supplier can manage the booking");
        }
    }

    private Booking bookingById(Long id) {
        return bookings.findById(id).orElseThrow(() -> new ApiException.NotFound("Booking not found"));
    }

    private AppUser userByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
    }
}
