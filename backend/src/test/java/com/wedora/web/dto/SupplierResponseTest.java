package com.wedora.web.dto;

import com.wedora.domain.AppUser;
import com.wedora.domain.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierResponseTest {
    @Test
    void doesNotUseAccountEmailAsPublicContactEmail() {
        AppUser owner = new AppUser();
        owner.setEmail("private-account@example.test");
        Supplier supplier = new Supplier();
        supplier.setOwner(owner);
        supplier.setBusinessName("Studio");
        supplier.setSlug("studio");
        supplier.setLocation("Athens");
        supplier.setContactPhone("+30123456789");
        supplier.setWhatsapp("+30987654321");
        supplier.setInstagram("https://instagram.com/studio");

        SupplierResponse response = SupplierResponse.from(supplier);

        assertThat(response.contactEmail()).isNull();
        assertThat(response.contactPhone()).isEqualTo("+30123456789");
        assertThat(response.whatsapp()).isEqualTo("+30987654321");
        assertThat(response.instagram()).isEqualTo("https://instagram.com/studio");
    }
}
