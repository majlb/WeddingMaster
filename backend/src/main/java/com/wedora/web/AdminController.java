package com.wedora.web;

import com.wedora.domain.Category;
import com.wedora.domain.Supplier;
import com.wedora.repository.BookingRepository;
import com.wedora.repository.CategoryRepository;
import com.wedora.repository.SupplierRepository;
import com.wedora.repository.UserRepository;
import com.wedora.web.dto.CategoryRequest;
import com.wedora.web.dto.CategoryResponse;
import com.wedora.web.dto.CategoryTranslation;
import com.wedora.web.dto.SupplierResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Transactional
public class AdminController {
    private final CategoryRepository categories;
    private final SupplierRepository suppliers;
    private final BookingRepository bookings;
    private final UserRepository users;

    public AdminController(CategoryRepository categories, SupplierRepository suppliers,
                           BookingRepository bookings, UserRepository users) {
        this.categories = categories;
        this.suppliers = suppliers;
        this.bookings = bookings;
        this.users = users;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categories.findAll().stream().map(CategoryResponse::from).toList();
    }

    @PostMapping("/categories")
    public CategoryResponse addCategory(@Valid @RequestBody CategoryRequest request) {
        if (categories.existsBySlug(request.slug())) throw new ApiException.Conflict("Category slug already exists");
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setTranslations(categoryTranslations(request));
        return CategoryResponse.from(categories.save(category));
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categories.findById(id).orElseThrow(() -> new ApiException.NotFound("Category not found"));
        if (!category.getSlug().equals(request.slug()) && categories.existsBySlug(request.slug())) {
            throw new ApiException.Conflict("Category slug already exists");
        }
        category.setName(request.name().trim());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setTranslations(categoryTranslations(request));
        return CategoryResponse.from(category);
    }

    private Map<String, Map<String, String>> categoryTranslations(CategoryRequest request) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        if (request.translations() == null
                || !request.translations().keySet().containsAll(List.of("en", "fr", "el", "ar"))) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Category name and description translations are required in English, French, Greek, and Arabic");
        }
        request.translations().forEach((language, translation) -> {
            if (!List.of("en", "fr", "el", "ar").contains(language)) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported translation language");
            }
            Map<String, String> fields = new LinkedHashMap<>();
            if (translation.name() == null || translation.name().isBlank()
                    || translation.description() == null || translation.description().isBlank()) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Category name and description translations cannot be blank");
            }
            fields.put("name", translation.name().trim());
            fields.put("description", translation.description().trim());
            result.put(language, fields);
        });
        return result;
    }

    @DeleteMapping("/categories/{id}")
    public void archiveCategory(@PathVariable Long id) {
        Category category = categories.findById(id).orElseThrow(() -> new ApiException.NotFound("Category not found"));
        category.setActive(false);
    }

    @GetMapping("/suppliers/pending")
    public List<SupplierResponse> pendingSuppliers() {
        return suppliers.findAll().stream().filter(supplier -> !supplier.isApproved() && supplier.isActive())
                .map(SupplierResponse::from).toList();
    }

    @PatchMapping("/suppliers/{id}/approval")
    public SupplierResponse approveSupplier(@PathVariable Long id, @RequestParam boolean approved) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        if (approved && supplier.getCategories().isEmpty()) {
            throw new ApiException.Conflict("The supplier must select at least one work category before approval");
        }
        supplier.setApproved(approved);
        if (approved) supplier.setActive(true);
        else if (!supplier.isApproved()) supplier.setActive(false);
        return SupplierResponse.from(supplier);
    }

    @GetMapping("/suppliers")
    public List<SupplierResponse> suppliers() {
        return suppliers.findAll().stream().map(SupplierResponse::from).toList();
    }

    @PatchMapping("/suppliers/{id}/activation")
    public SupplierResponse toggleSupplierActivation(@PathVariable Long id, @RequestParam boolean active) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new ApiException.NotFound("Supplier not found"));
        supplier.setActive(active);
        return SupplierResponse.from(supplier);
    }

    @GetMapping("/analytics")
    public Map<String, Long> analytics() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("users", users.count());
        result.put("suppliers", suppliers.count());
        result.put("approvedSuppliers", suppliers.countByApprovedTrueAndActiveTrue());
        result.put("bookings", bookings.count());
        return result;
    }
}
