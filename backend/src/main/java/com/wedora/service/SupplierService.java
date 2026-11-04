package com.wedora.service;

import com.wedora.domain.Supplier;
import com.wedora.domain.CountryCodes;
import com.wedora.repository.SupplierRepository;
import com.wedora.repository.UserRepository;
import com.wedora.repository.BookingRepository;
import com.wedora.repository.MessageRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.SupplierResponse;
import com.wedora.web.dto.SupplierUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SupplierService {
    private final SupplierRepository suppliers;
    private final UserRepository users;
    private final BookingRepository bookings;
    private final MessageRepository messages;

    public SupplierService(SupplierRepository suppliers, UserRepository users,
                           BookingRepository bookings, MessageRepository messages) {
        this.suppliers = suppliers;
        this.users = users;
        this.bookings = bookings;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> list(String category, String location, BigDecimal maxPrice, Double minRating) {
        return suppliers.findByApprovedTrueAndActiveTrue().stream()
                .filter(s -> category == null || s.getCategory().getSlug().equalsIgnoreCase(category))
                .filter(s -> location == null || s.getLocation().toLowerCase(Locale.ROOT).contains(location.toLowerCase(Locale.ROOT)))
                .filter(s -> maxPrice == null || (s.getStartingPrice() != null && s.getStartingPrice().compareTo(maxPrice) <= 0))
                .filter(s -> minRating == null || (s.getRating() != null && s.getRating() >= minRating))
                .map(SupplierResponse::from)
                .toList();
    }

    @Transactional
    public SupplierResponse getApproved(Long id) {
        Supplier supplier = suppliers.findByIdAndApprovedTrueAndActiveTrue(id)
                .orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        suppliers.incrementProfileVisits(id);
        return SupplierResponse.from(supplier);
    }

    @Transactional(readOnly = true)
    public void requireApproved(Long id) {
        if (suppliers.findByIdAndApprovedTrueAndActiveTrue(id).isEmpty()) {
            throw new ApiException.NotFound("Supplier not found");
        }
    }

    @Transactional
    public SupplierResponse getApprovedBySlug(String slug) {
        Supplier supplier = suppliers.findBySlugAndApprovedTrueAndActiveTrue(slug)
                .orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        suppliers.incrementProfileVisits(supplier.getId());
        return SupplierResponse.from(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listOwned(String email) {
        return suppliers.findByOwnerId(ownerIdForEmail(email)).stream().map(SupplierResponse::from).toList();
    }

    @Transactional
    public SupplierResponse updateOwned(Long id, String email, SupplierUpdateRequest request) {
        Supplier supplier = suppliers.findById(id)
                .orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        ensureOwner(supplier, email);
        if (request.countryCodes() != null) supplier.setCountryCodes(CountryCodes.normalize(request.countryCodes()));
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together");
        }
        if (request.latitude() != null) {
            if (request.latitude() < -90 || request.latitude() > 90
                    || request.longitude() < -180 || request.longitude() > 180) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Map coordinates are out of range");
            }
            supplier.setLatitude(request.latitude());
            supplier.setLongitude(request.longitude());
        }
        if (request.businessName() != null) supplier.setBusinessName(request.businessName().trim());
        if (request.location() != null) supplier.setLocation(request.location().trim());
        if (request.description() != null) supplier.setDescription(request.description());
        if (request.services() != null) supplier.setServices(request.services());
        if (request.availability() != null) supplier.setAvailability(request.availability());
        if (request.startingPrice() != null) {
            if (request.startingPrice().signum() < 0) throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Price cannot be negative");
            supplier.setStartingPrice(request.startingPrice());
        }
        if (request.currency() != null) supplier.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        if (request.contactPhone() != null) supplier.setContactPhone(request.contactPhone());
        if (request.website() != null) supplier.setWebsite(request.website());
        if (request.localizedContent() != null) {
            Map<String, Map<String, String>> localizedContent = new java.util.LinkedHashMap<>();
            request.localizedContent().forEach((language, content) -> {
                if (!List.of("en", "fr", "el", "ar").contains(language)) {
                    throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported translation language");
                }
                Map<String, String> fields = new java.util.LinkedHashMap<>();
                putTranslation(fields, "businessName", content.businessName());
                putTranslation(fields, "location", content.location());
                putTranslation(fields, "description", content.description());
                putTranslation(fields, "services", content.services());
                putTranslation(fields, "availability", content.availability());
                localizedContent.put(language, fields);
            });
            supplier.setLocalizedContent(localizedContent);
        }
        return SupplierResponse.from(supplier);
    }

    private void putTranslation(Map<String, String> fields, String name, String value) {
        if (value != null && !value.isBlank()) fields.put(name, value.trim());
    }

    @Transactional(readOnly = true)
    public Supplier requireOwned(Long id, String email) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        ensureOwner(supplier, email);
        return supplier;
    }

    @Transactional(readOnly = true)
    public SupplierAnalytics analytics(Long id, String email) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        ensureOwner(supplier, email);
        long bookingCount = bookings.findBySupplierId(id).size();
        return new SupplierAnalytics(supplier.getProfileVisits(), bookingCount, messages.countByBookingSupplierId(id));
    }

    public record SupplierAnalytics(long visits, long bookings, long messages) {}

    private void ensureOwner(Supplier supplier, String email) {
        if (!supplier.getOwner().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException.Forbidden("You can only manage your own supplier profile");
        }
    }

    private Long ownerIdForEmail(String email) {
        Long ownerId = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException.Unauthorized("Account not found")).getId();
        if (suppliers.findByOwnerId(ownerId).isEmpty()) {
            throw new ApiException.NotFound("Supplier profile not found");
        }
        return ownerId;
    }
}
