package com.wedora.service;

import com.wedora.domain.Supplier;
import com.wedora.domain.CountryCodes;
import com.wedora.domain.Category;
import com.wedora.repository.CategoryRepository;
import com.wedora.repository.SupplierRepository;
import com.wedora.repository.SupplierServiceOfferingRepository;
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
    private final CategoryRepository categories;
    private final SupplierServiceOfferingRepository serviceOfferings;

    public SupplierService(SupplierRepository suppliers, UserRepository users,
                           BookingRepository bookings, MessageRepository messages,
                           CategoryRepository categories, SupplierServiceOfferingRepository serviceOfferings) {
        this.suppliers = suppliers;
        this.users = users;
        this.bookings = bookings;
        this.messages = messages;
        this.categories = categories;
        this.serviceOfferings = serviceOfferings;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> list(String category, String location, BigDecimal maxPrice, Double minRating) {
        return suppliers.findByApprovedTrueAndActiveTrue().stream()
                .filter(s -> category == null || s.getCategories().stream()
                        .anyMatch(assigned -> assigned.getSlug().equalsIgnoreCase(category)))
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
        if (request.contactEmail() != null) supplier.setContactEmail(cleanOptionalContact(request.contactEmail()));
        if (request.contactPhone() != null) supplier.setContactPhone(cleanOptionalContact(request.contactPhone()));
        if (request.whatsapp() != null) supplier.setWhatsapp(cleanOptionalContact(request.whatsapp()));
        if (request.website() != null) supplier.setWebsite(cleanOptionalContact(request.website()));
        if (request.facebook() != null) supplier.setFacebook(cleanOptionalContact(request.facebook()));
        if (request.instagram() != null) supplier.setInstagram(cleanOptionalContact(request.instagram()));
        if (request.tiktok() != null) supplier.setTiktok(cleanOptionalContact(request.tiktok()));
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

    @Transactional
    public SupplierResponse updateCategories(Long id, String email, List<Long> categoryIds) {
        Supplier supplier = suppliers.findById(id)
                .orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        ensureOwner(supplier, email);
        if (categoryIds == null || categoryIds.stream().distinct().count() != categoryIds.size()) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Choose each supplier category only once");
        }
        List<Category> requestedCategories = categories.findAllById(categoryIds).stream()
                .filter(Category::isActive)
                .toList();
        if (requestedCategories.size() != categoryIds.size()) {
            throw new ApiException.NotFound("One or more supplier categories are unavailable");
        }
        for (Category existingCategory : supplier.getCategories()) {
            if (!categoryIds.contains(existingCategory.getId())
                    && serviceOfferings.existsBySupplierIdAndCategoryId(id, existingCategory.getId())) {
                throw new ApiException.Conflict(
                        "Move or delete services in a category before removing it from your supplier profile");
            }
        }
        Map<Long, Category> categoriesById = requestedCategories.stream()
                .collect(java.util.stream.Collectors.toMap(Category::getId, category -> category));
        List<Category> orderedCategories = categoryIds.stream().map(categoriesById::get).toList();
        supplier.setCategories(new java.util.ArrayList<>(orderedCategories));
        supplier.setCategory(orderedCategories.isEmpty() ? null : orderedCategories.get(0));
        return SupplierResponse.from(supplier);
    }

    private void putTranslation(Map<String, String> fields, String name, String value) {
        if (value != null && !value.isBlank()) fields.put(name, value.trim());
    }

    private String cleanOptionalContact(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
