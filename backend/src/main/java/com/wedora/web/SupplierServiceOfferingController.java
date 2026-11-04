package com.wedora.web;

import com.wedora.service.SupplierServiceOfferingService;
import com.wedora.web.dto.SupplierServiceRequest;
import com.wedora.web.dto.SupplierServiceResponse;
import com.wedora.web.dto.AvailabilityDateResponse;
import com.wedora.web.dto.AvailabilityBlockRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierServiceOfferingController {
    private final SupplierServiceOfferingService service;

    public SupplierServiceOfferingController(SupplierServiceOfferingService service) {
        this.service = service;
    }

    @GetMapping("/{supplierId}/services")
    public List<SupplierServiceResponse> list(@PathVariable Long supplierId) {
        return service.listPublic(supplierId);
    }

    @GetMapping("/me/{supplierId}/services")
    public List<SupplierServiceResponse> listOwned(@PathVariable Long supplierId,
                                                    @AuthenticationPrincipal String email) {
        return service.listOwned(supplierId, email);
    }

    @GetMapping("/{supplierId}/services/{serviceId}/availability")
    public List<AvailabilityDateResponse> availability(@PathVariable Long supplierId,
                                                       @PathVariable Long serviceId,
                                                       @RequestParam LocalDate from,
                                                       @RequestParam LocalDate to) {
        return service.listPublicAvailability(supplierId, serviceId, from, to);
    }

    @GetMapping("/me/{supplierId}/services/{serviceId}/availability")
    public List<AvailabilityDateResponse> ownedAvailability(@PathVariable Long supplierId,
                                                            @PathVariable Long serviceId,
                                                            @RequestParam LocalDate from,
                                                            @RequestParam LocalDate to,
                                                            @AuthenticationPrincipal String email) {
        return service.listOwnedAvailability(supplierId, serviceId, email, from, to);
    }

    @PostMapping("/me/{supplierId}/services/{serviceId}/availability")
    public AvailabilityDateResponse blockDate(@PathVariable Long supplierId, @PathVariable Long serviceId,
                                              @AuthenticationPrincipal String email,
                                              @Valid @RequestBody AvailabilityBlockRequest request) {
        return service.blockDate(supplierId, serviceId, email, request.date());
    }

    @DeleteMapping("/me/{supplierId}/services/{serviceId}/availability/{date}")
    public void unblockDate(@PathVariable Long supplierId, @PathVariable Long serviceId,
                            @PathVariable LocalDate date, @AuthenticationPrincipal String email) {
        service.unblockDate(supplierId, serviceId, email, date);
    }

    @PostMapping(value = "/{supplierId}/services", consumes = "multipart/form-data")
    public SupplierServiceResponse create(@PathVariable Long supplierId,
                                         @AuthenticationPrincipal String email,
                                         @Valid @RequestPart("service") SupplierServiceRequest request,
                                         @RequestPart(value = "image", required = false) MultipartFile image) {
        return service.create(supplierId, email, request, image);
    }

    @PutMapping(value = "/{supplierId}/services/{serviceId}", consumes = "multipart/form-data")
    public SupplierServiceResponse update(@PathVariable Long supplierId, @PathVariable Long serviceId,
                                          @AuthenticationPrincipal String email,
                                          @Valid @RequestPart("service") SupplierServiceRequest request,
                                          @RequestPart(value = "image", required = false) MultipartFile image) {
        return service.update(supplierId, serviceId, email, request, image);
    }

    @DeleteMapping("/{supplierId}/services/{serviceId}")
    public void delete(@PathVariable Long supplierId, @PathVariable Long serviceId,
                       @AuthenticationPrincipal String email) {
        service.delete(supplierId, serviceId, email);
    }
}
