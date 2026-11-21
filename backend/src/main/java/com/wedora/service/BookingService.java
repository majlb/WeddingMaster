package com.wedora.service;

import com.wedora.domain.*;
import com.wedora.repository.*;
import com.wedora.web.ApiException;
import com.wedora.web.dto.BookingRequest;
import com.wedora.web.dto.BookingResponse;
import com.wedora.web.dto.QuoteRequest;
import org.springframework.scheduling.annotation.Scheduled;
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
        if (request.quantity() == null || request.quantity() < 1) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "A positive quantity is required");
        }
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
            if (request.quantity() < offering.getMinimumAmount() || request.quantity() > offering.getMaximumAmount()) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Quantity is outside the service limits");
            }
        }
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setSupplier(supplier);
        booking.setServiceOffering(offering);
        booking.setEventDate(request.eventDate());
        booking.setNotes(request.notes());
        booking.setCurrency(supplier.getCurrency());
        booking.setQuantity(request.quantity());
        if (offering != null) {
            booking.setServicePrice(offering.getPrice());
            booking.setServicePriceCurrency(supplier.getCurrency());
            booking.setServicePricingType(offering.getPricingType());
        }
        booking.addHistory(BookingStatus.NEW_REQUEST, "CUSTOMER", request.notes());
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
        Booking booking = bookingForUpdate(bookingId);
        ensureSupplierOwner(booking, email);
        if (booking.getStatus() != BookingStatus.REQUESTED && booking.getStatus() != BookingStatus.ACCEPTED
                && booking.getStatus() != BookingStatus.UNDER_REVIEW) {
            throw new ApiException.Conflict("Only requested or accepted bookings can be quoted");
        }
        booking.setQuotedAmountCents(request.amountCents());
        if (request.currency() != null) {
            if (!request.currency().matches("[A-Za-z]{3}")) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Currency must be a three-letter ISO code");
            booking.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        }
        if (booking.getStatus() == BookingStatus.UNDER_REVIEW) {
            booking.addHistory(BookingStatus.SENT_TO_COUPLE, "SUPPLIER", null);
        } else {
            booking.setStatus(BookingStatus.QUOTED);
        }
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
        booking.setStatus(BookingStatus.ACCEPTED);
        updateChecklist(booking, "Supplier accepted", "Awaiting supplier quote");
        notifications.supplierAccepted(booking);
        userNotifications.notify(booking.getCustomer(), booking, "BOOKING_ACCEPTED",
                booking.getSupplier().getBusinessName());
        return BookingResponse.from(booking);
    }

    @Transactional
        public BookingResponse review(Long bookingId, String email, String comment) {
            Booking booking = bookingForUpdate(bookingId);
            if (booking.getStatus() == BookingStatus.NEW_REQUEST) {
                ensureSupplierOwner(booking, email);
                booking.addHistory(BookingStatus.UNDER_REVIEW, "SUPPLIER", null);
                userNotifications.notify(booking.getCustomer(), booking, "REQUEST_UPDATED", booking.getSupplier().getBusinessName());
            } else if (booking.getStatus() == BookingStatus.SENT_TO_COUPLE) {
                ensureCustomer(booking, email);
                requireComment(comment);
                booking.addHistory(BookingStatus.UNDER_REVIEW, "CUSTOMER", comment.trim());
                userNotifications.notify(booking.getSupplier().getOwner(), booking, "REQUEST_UPDATED", booking.getCustomer().getName());
            } else {
                throw new ApiException.Conflict("This request cannot be moved under review");
            }
            return BookingResponse.from(booking);
        }

        @Transactional
        public BookingResponse respondToQuote(Long bookingId, String email) {
            Booking booking = bookingForUpdate(bookingId);
            ensureCustomer(booking, email);
            if (booking.getStatus() != BookingStatus.SENT_TO_COUPLE) {
                throw new ApiException.Conflict("Only a sent quotation can be accepted");
            }
            booking.addHistory(BookingStatus.ACCEPTED_BY_COUPLE, "CUSTOMER", null);
            userNotifications.notify(booking.getSupplier().getOwner(), booking, "REQUEST_UPDATED", booking.getCustomer().getName());
            return BookingResponse.from(booking);
        }

        @Transactional
        public BookingResponse finalizeRequest(Long bookingId, String email) {
            Booking booking = bookingForUpdate(bookingId);
            ensureSupplierOwner(booking, email);
            if (booking.getStatus() != BookingStatus.ACCEPTED_BY_COUPLE) {
                throw new ApiException.Conflict("Only a quotation accepted by the couple can be finalized");
            }
            if (booking.getSupplier().getTimeZone() == null) {
                throw new ApiException.Conflict("Set your business time zone before accepting the request");
            }
            booking.addHistory(BookingStatus.WAITING_PAYMENT, "SUPPLIER", null);
            userNotifications.notify(booking.getCustomer(), booking, "REQUEST_UPDATED", booking.getSupplier().getBusinessName());
            updateChecklist(booking, "Booking accepted", "Payment due");
            return BookingResponse.from(booking);
        }

        @Transactional
        public BookingResponse declineRequest(Long bookingId, String email, String comment) {
            Booking booking = bookingForUpdate(bookingId);
            requireComment(comment);
            if (booking.getStatus() == BookingStatus.SENT_TO_COUPLE) {
                ensureCustomer(booking, email);
                booking.addHistory(BookingStatus.DECLINED_BY_COUPLE, "CUSTOMER", comment.trim());
                userNotifications.notify(booking.getSupplier().getOwner(), booking, "REQUEST_UPDATED", booking.getCustomer().getName());
            } else if (booking.getStatus() == BookingStatus.NEW_REQUEST || booking.getStatus() == BookingStatus.UNDER_REVIEW
                    || booking.getStatus() == BookingStatus.ACCEPTED_BY_COUPLE) {
                ensureSupplierOwner(booking, email);
                booking.addHistory(BookingStatus.DECLINED_BY_SUPPLIER, "SUPPLIER", comment.trim());
                userNotifications.notify(booking.getCustomer(), booking, "REQUEST_UPDATED", booking.getSupplier().getBusinessName());
            } else {
                throw new ApiException.Conflict("This request cannot be declined");
            }
            updateChecklist(booking, "Request declined", "No payment due");
            return BookingResponse.from(booking);
        }

        @Transactional
        public void paymentSucceeded(Long bookingId) {
            Booking booking = bookingForUpdate(bookingId);
            if (booking.getStatus() == BookingStatus.WAITING_PAYMENT) {
                booking.addHistory(BookingStatus.ACTIVE, "SYSTEM", null);
                updateChecklist(booking, "Booking active", "Paid");
                userNotifications.notify(booking.getCustomer(), booking, "REQUEST_UPDATED", "Wedmaster");
                userNotifications.notify(booking.getSupplier().getOwner(), booking, "REQUEST_UPDATED", "Wedmaster");
            }
        }

        @Scheduled(fixedDelay = 3600000)
        @Transactional
        public void closePastRequests() {
            for (Booking candidate : bookings.findByStatus(BookingStatus.ACTIVE)) {
                Booking booking = bookingForUpdate(candidate.getId());
                String zone = booking.getSupplier().getTimeZone();
                if (booking.getStatus() == BookingStatus.ACTIVE && zone != null
                        && booking.getEventDate().isBefore(java.time.LocalDate.now(java.time.ZoneId.of(zone)))) {
                    booking.addHistory(BookingStatus.CLOSED, "SYSTEM", null);
                    userNotifications.notify(booking.getCustomer(), booking, "REQUEST_UPDATED", "Wedmaster");
                    userNotifications.notify(booking.getSupplier().getOwner(), booking, "REQUEST_UPDATED", "Wedmaster");
                }
            }
        }

        private void requireComment(String comment) {
            if (comment == null || comment.isBlank() || comment.length() > 3000) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "A comment is required");
            }
        }

        private void ensureCustomer(Booking booking, String email) {
            if (!booking.getCustomer().getEmail().equalsIgnoreCase(email)) {
                throw new ApiException.Forbidden("Only the customer can respond to this request");
            }
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

    @Transactional
    public Booking requireParticipantForUpdate(Long bookingId, String email) {
        Booking booking = bookingForUpdate(bookingId);
        boolean customer = booking.getCustomer().getEmail().equalsIgnoreCase(email);
        boolean supplier = booking.getSupplier().getOwner().getEmail().equalsIgnoreCase(email);
        if (!customer && !supplier) throw new ApiException.Forbidden("You are not part of this booking");
        return booking;
    }

    private void updateChecklist(Booking booking, String contract, String payment) {
        checklist.findByBookingId(booking.getId()).forEach(item -> {
            item.setCompleted(booking.getStatus() == BookingStatus.CONFIRMED
                    || booking.getStatus() == BookingStatus.WAITING_PAYMENT
                    || booking.getStatus() == BookingStatus.ACTIVE || booking.getStatus() == BookingStatus.CLOSED);
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

    private Booking bookingForUpdate(Long id) {
        return bookings.findByIdForUpdate(id).orElseThrow(() -> new ApiException.NotFound("Booking not found"));
    }

    private AppUser userByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found"));
    }
}
