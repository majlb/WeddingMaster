package com.wedora.service;

import com.wedora.domain.Supplier;
import com.wedora.domain.SupplierAvailabilityBlock;
import com.wedora.domain.Category;
import com.wedora.domain.SupplierServiceOffering;
import com.wedora.repository.BookingRepository;
import com.wedora.repository.SupplierAvailabilityBlockRepository;
import com.wedora.repository.SupplierServiceOfferingRepository;
import com.wedora.web.ApiException;
import com.wedora.web.dto.AvailabilityDateResponse;
import com.wedora.web.dto.SupplierServiceRequest;
import com.wedora.web.dto.SupplierServiceResponse;
import com.wedora.web.dto.SupplierServiceTranslationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class SupplierServiceOfferingService {
    private static final long MAX_AVAILABILITY_RANGE_DAYS = 366;
    private static final List<String> SUPPORTED_LANGUAGES = List.of("en", "fr", "el", "ar");
    private final SupplierServiceOfferingRepository services;
    private final SupplierAvailabilityBlockRepository availabilityBlocks;
    private final BookingRepository bookings;
    private final SupplierService suppliers;
    private final MediaService mediaService;

    public SupplierServiceOfferingService(SupplierServiceOfferingRepository services,
                                          SupplierAvailabilityBlockRepository availabilityBlocks,
                                          BookingRepository bookings,
                                          SupplierService suppliers, MediaService mediaService) {
        this.services = services;
        this.availabilityBlocks = availabilityBlocks;
        this.bookings = bookings;
        this.suppliers = suppliers;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public List<SupplierServiceResponse> listPublic(Long supplierId) {
        suppliers.requireApproved(supplierId);
        return services.findBySupplierIdOrderByIdAsc(supplierId).stream()
                .map(SupplierServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierServiceResponse> listOwned(Long supplierId, String email) {
        suppliers.requireOwned(supplierId, email);
        return services.findBySupplierIdOrderByIdAsc(supplierId).stream()
                .map(SupplierServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilityDateResponse> listPublicAvailability(Long supplierId, Long serviceId,
                                                                  LocalDate from, LocalDate to) {
        suppliers.requireApproved(supplierId);
        requireOffering(supplierId, serviceId);
        return availability(serviceId, from, to);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityDateResponse> listOwnedAvailability(Long supplierId, Long serviceId, String email,
                                                                 LocalDate from, LocalDate to) {
        suppliers.requireOwned(supplierId, email);
        requireOffering(supplierId, serviceId);
        return availability(serviceId, from, to);
    }

    @Transactional
    public AvailabilityDateResponse blockDate(Long supplierId, Long serviceId, String email, LocalDate date) {
        suppliers.requireOwned(supplierId, email);
        SupplierServiceOffering offering = lockedOffering(supplierId, serviceId);
        if (date.isBefore(LocalDate.now())) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Past dates cannot be blocked");
        }
        if (availabilityBlocks.existsByServiceOfferingIdAndBlockedDate(serviceId, date)) {
            throw new ApiException.Conflict("This date is already blocked or booked for the selected service");
        }
        SupplierAvailabilityBlock block = new SupplierAvailabilityBlock();
        block.setServiceOffering(offering);
        block.setBlockedDate(date);
        availabilityBlocks.save(block);
        return new AvailabilityDateResponse(date, "BLOCKED");
    }

    @Transactional
    public void unblockDate(Long supplierId, Long serviceId, String email, LocalDate date) {
        suppliers.requireOwned(supplierId, email);
        lockedOffering(supplierId, serviceId);
        SupplierAvailabilityBlock block = availabilityBlocks.findByServiceOfferingIdAndBlockedDate(serviceId, date)
                .orElseThrow(() -> new ApiException.NotFound("Blocked date not found"));
        if (block.getBooking() != null) {
            throw new ApiException.Conflict("A confirmed booking reserves this date and cannot be unblocked");
        }
        availabilityBlocks.delete(block);
    }

    @Transactional
    public SupplierServiceResponse create(Long supplierId, String email,
                                          SupplierServiceRequest request, MultipartFile image) {
        Supplier supplier = suppliers.requireOwned(supplierId, email);
        validate(request);
        SupplierServiceOffering offering = new SupplierServiceOffering();
        offering.setSupplier(supplier);
        offering.setCategory(requireAssignedCategory(supplier, request.categoryId()));
        applyRequest(offering, request);
        if (image != null && !image.isEmpty()) {
            offering.setImageUrl(mediaService.storeServiceImage(supplierId, email, image));
        }
        return SupplierServiceResponse.from(services.save(offering));
    }

    @Transactional
    public SupplierServiceResponse update(Long supplierId, Long serviceId, String email,
                                          SupplierServiceRequest request, MultipartFile image) {
        Supplier supplier = suppliers.requireOwned(supplierId, email);
        SupplierServiceOffering offering = services.findByIdAndSupplierId(serviceId, supplierId)
                .orElseThrow(() -> new ApiException.NotFound("Service not found"));
        validate(request);
        offering.setCategory(requireAssignedCategory(supplier, request.categoryId()));
        applyRequest(offering, request);
        if (image != null && !image.isEmpty()) {
            offering.setImageUrl(mediaService.storeServiceImage(supplierId, email, image));
        }
        return SupplierServiceResponse.from(offering);
    }

    @Transactional
    public void delete(Long supplierId, Long serviceId, String email) {
        suppliers.requireOwned(supplierId, email);
        SupplierServiceOffering offering = services.findByIdAndSupplierId(serviceId, supplierId)
                .orElseThrow(() -> new ApiException.NotFound("Service not found"));
        if (bookings.existsByServiceOfferingId(serviceId)) {
            throw new ApiException.Conflict("This service cannot be deleted because it has booking history");
        }
        availabilityBlocks.deleteByServiceOfferingId(serviceId);
        services.delete(offering);
    }

    private List<AvailabilityDateResponse> availability(Long serviceId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)
                || ChronoUnit.DAYS.between(from, to) > MAX_AVAILABILITY_RANGE_DAYS) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Choose a valid date range of at most 12 months");
        }
        return availabilityBlocks.findByServiceOfferingIdAndBlockedDateBetweenOrderByBlockedDateAsc(serviceId, from, to)
                .stream()
                .map(block -> new AvailabilityDateResponse(block.getBlockedDate(),
                        block.getBooking() == null ? "BLOCKED" : "BOOKED"))
                .toList();
    }

    private SupplierServiceOffering requireOffering(Long supplierId, Long serviceId) {
        return services.findByIdAndSupplierId(serviceId, supplierId)
                .orElseThrow(() -> new ApiException.NotFound("Supplier service not found"));
    }

    private SupplierServiceOffering lockedOffering(Long supplierId, Long serviceId) {
        SupplierServiceOffering offering = services.findByIdForUpdate(serviceId)
                .orElseThrow(() -> new ApiException.NotFound("Supplier service not found"));
        if (!offering.getSupplier().getId().equals(supplierId)) {
            throw new ApiException.NotFound("Supplier service not found");
        }
        return offering;
    }

    private void validate(SupplierServiceRequest request) {
        if (request == null || request.categoryId() == null || request.translations() == null
                || request.translations().size() != SUPPORTED_LANGUAGES.size()
                || !request.translations().keySet().containsAll(SUPPORTED_LANGUAGES)) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Provide service translations in English, French, Greek, and Arabic");
        }

        for (String language : SUPPORTED_LANGUAGES) {
            SupplierServiceTranslationRequest translation = request.translations().get(language);
            if (translation == null || translation.name() == null || translation.name().isBlank()) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Service name is required in every language");
            }
            if (translation.name().length() > 150) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Service names must be 150 characters or fewer");
            }
            if (translation.description() != null && translation.description().length() > 2000) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Service description must be 2000 characters or fewer");
            }
        }
        if (request.price() != null && request.price().signum() < 0) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Service price cannot be negative");
        }
    }

    private Category requireAssignedCategory(Supplier supplier, Long categoryId) {
        return supplier.getCategories().stream()
                .filter(category -> category.getId().equals(categoryId) && category.isActive())
                .findFirst()
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Choose a category assigned to your supplier profile"));
    }

    private void applyRequest(SupplierServiceOffering offering, SupplierServiceRequest request) {
        Map<String, Map<String, String>> localizedContent = new LinkedHashMap<>(offering.getLocalizedContent());
        for (String language : SUPPORTED_LANGUAGES) {
            SupplierServiceTranslationRequest translation = request.translations().get(language);
            Map<String, String> translated = new LinkedHashMap<>();
            translated.put("name", translation.name().trim());
            translated.put("description", translation.description() == null ? "" : translation.description().trim());
            localizedContent.put(language, translated);
        }
        SupplierServiceTranslationRequest english = request.translations().get("en");
        offering.setName(english.name().trim());
        offering.setDescription(english.description() == null ? "" : english.description().trim());
        offering.setLocalizedContent(localizedContent);
        offering.setPrice(request.price());
    }
}
