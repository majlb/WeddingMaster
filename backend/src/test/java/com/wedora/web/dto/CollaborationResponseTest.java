package com.wedora.web.dto;

import com.wedora.domain.Category;
import com.wedora.domain.CollaborationItem;
import com.wedora.domain.Supplier;
import com.wedora.domain.SupplierCollaboration;
import com.wedora.domain.SupplierServiceOffering;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CollaborationResponseTest {
    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void includesServiceCategoryInCollaborationItems() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Supplier supplier = new Supplier();
        supplier.setId(10L);
        supplier.setCurrency("USD");
        supplier.setBusinessName("Beauty studio");

        Category category = new Category();
        category.setName("Hair");
        category.setSlug("hair");

        SupplierServiceOffering service = new SupplierServiceOffering();
        service.setId(20L);
        service.setSupplier(supplier);
        service.setCategory(category);
        service.setName("Bridal hair styling");

        CollaborationItem item = new CollaborationItem();
        item.setId(30L);
        item.setServiceOffering(service);

        SupplierCollaboration collaboration = new SupplierCollaboration();
        collaboration.setId(40L);
        collaboration.setName("Hair and makeup");
        collaboration.setCreatedBySupplier(supplier);

        CollaborationResponse response = CollaborationResponse.from(
                collaboration, List.of(), List.of(item), false, true);

        assertThat(response.items()).singleElement().satisfies(responseItem -> {
            assertThat(responseItem.categoryName()).isEqualTo("Hair");
            assertThat(responseItem.categorySlug()).isEqualTo("hair");
        });
    }
}
