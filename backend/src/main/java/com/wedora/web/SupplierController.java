package com.wedora.web;

import com.wedora.service.SupplierService;
import com.wedora.web.dto.SupplierResponse;
import com.wedora.web.dto.SupplierUpdateRequest;
import com.wedora.web.dto.SupplierUpdateCategoriesRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating) {
        return supplierService.list(category, location, maxPrice, minRating);
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable Long id) {
        return supplierService.getApproved(id);
    }

    @GetMapping("/slug/{slug}")
    public SupplierResponse getBySlug(@PathVariable String slug) {
        return supplierService.getApprovedBySlug(slug);
    }

    @GetMapping("/me")
    public List<SupplierResponse> owned(@AuthenticationPrincipal String email) {
        return supplierService.listOwned(email);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @AuthenticationPrincipal String email,
                                   @Valid @RequestBody SupplierUpdateRequest request) {
        return supplierService.updateOwned(id, email, request);
    }

    @PutMapping("/{id}/categories")
    public SupplierResponse updateCategories(@PathVariable Long id, @AuthenticationPrincipal String email,
                                             @Valid @RequestBody SupplierUpdateCategoriesRequest request) {
        return supplierService.updateCategories(id, email, request.categoryIds());
    }

    @GetMapping("/me/{id}/analytics")
    public SupplierService.SupplierAnalytics analytics(@PathVariable Long id, @AuthenticationPrincipal String email) {
        return supplierService.analytics(id, email);
    }
}
